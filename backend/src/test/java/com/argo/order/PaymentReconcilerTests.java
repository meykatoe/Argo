package com.argo.order;

import static com.argo.order.OrderTestSupport.EMAIL;
import static com.argo.order.OrderTestSupport.card;
import static com.argo.order.OrderTestSupport.request;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.argo.card.Card;
import com.argo.card.CardRepository;
import com.argo.card.CardSetRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class PaymentReconcilerTests {

	@Autowired
	OrderService orders;
	@Autowired
	PaymentService payments;
	@Autowired
	PaymentRepository paymentRepo;
	@Autowired
	EcpayCheckout ecpay;
	@Autowired
	CardRepository cards;
	@Autowired
	CardSetRepository sets;
	@Autowired
	JdbcTemplate jdbc;
	@PersistenceContext
	EntityManager em;

	Card a;
	final Map<String, PaymentQuery.Info> answers = new LinkedHashMap<>();
	int calls;
	PaymentReconciler reconciler;

	@BeforeEach
	void setUp() {
		a = card(cards, sets, jdbc, "TS-61", "A", 10.0, 5);
		PaymentQuery stub = tradeNo -> {
			calls++;
			return Optional.ofNullable(answers.get(tradeNo));
		};
		reconciler = new PaymentReconciler(paymentRepo, payments, stub, 5, 30);
		em.clear();
	}

	// 建立一筆已超過等待時間的綠界待付款
	private String pendingEcpay(OrderView o) {
		String trade = payments.start(o.orderNo(), EMAIL).tradeNo();
		em.flush();
		jdbc.update("update payment set method = 'ECPAY_CREDIT', created_at = now() - interval '10 minutes' "
				+ "where trade_no = ?", trade);
		em.clear();
		return trade;
	}

	private OrderView newOrder() {
		return orders.create(request(a.getId(), 1L), "en", null);
	}

	@Test
	void lostNoticeIsRecoveredByQuery() {
		OrderView o = newOrder();
		String trade = pendingEcpay(o);
		answers.put(trade, new PaymentQuery.Info("1", o.totalTwd(), "2610140000009", "4242"));
		reconciler.run();
		em.flush();
		em.clear();
		OrderView after = orders.get(o.orderNo(), EMAIL);
		assertEquals(OrderStatus.PAID, after.status());
		assertEquals("4242", after.payment().cardLast4());
	}

	@Test
	void amountMismatchFailsPayment() {
		OrderView o = newOrder();
		String trade = pendingEcpay(o);
		answers.put(trade, new PaymentQuery.Info("1", o.totalTwd() - 1, "2610140000009", "4242"));
		reconciler.run();
		em.flush();
		em.clear();
		OrderView after = orders.get(o.orderNo(), EMAIL);
		assertEquals(OrderStatus.PENDING_PAYMENT, after.status());
		assertEquals("AMOUNT_MISMATCH", after.payment().failureCode());
	}

	@Test
	void repeatedRunHasNoSideEffect() {
		OrderView o = newOrder();
		String trade = pendingEcpay(o);
		answers.put(trade, new PaymentQuery.Info("1", o.totalTwd(), "2610140000009", "4242"));
		reconciler.run();
		reconciler.run();
		em.flush();
		assertEquals(1, jdbc.queryForObject(
				"select count(*) from payment where trade_no = ? and status = 'SUCCEEDED'", Integer.class, trade));
		assertEquals("NONE", jdbc.queryForObject(
				"select refund_status from payment where trade_no = ?", String.class, trade));
	}

	@Test
	void unpaidAnswerLeavesPending() {
		OrderView o = newOrder();
		String trade = pendingEcpay(o);
		answers.put(trade, new PaymentQuery.Info("0", o.totalTwd(), null, null));
		reconciler.run();
		em.flush();
		em.clear();
		assertEquals(OrderStatus.PENDING_PAYMENT, orders.get(o.orderNo(), EMAIL).status());
		assertEquals("PENDING", orders.get(o.orderNo(), EMAIL).payment().status());
	}

	@Test
	void failedAnswerFailsPaymentButKeepsOrder() {
		OrderView o = newOrder();
		String trade = pendingEcpay(o);
		answers.put(trade, new PaymentQuery.Info("10200095", o.totalTwd(), null, null));
		reconciler.run();
		em.flush();
		em.clear();
		OrderView after = orders.get(o.orderNo(), EMAIL);
		assertEquals(OrderStatus.PENDING_PAYMENT, after.status());
		assertEquals("ECPAY_10200095", after.payment().failureCode());
	}

	@Test
	void youngPaymentIsNotQueried() {
		OrderView o = newOrder();
		String trade = payments.start(o.orderNo(), EMAIL).tradeNo();
		em.flush();
		jdbc.update("update payment set method = 'ECPAY_CREDIT' where trade_no = ?", trade);
		em.clear();
		answers.put(trade, new PaymentQuery.Info("1", o.totalTwd(), "x", "1111"));
		reconciler.run();
		assertEquals(0, calls);
		assertEquals(OrderStatus.PENDING_PAYMENT, orders.get(o.orderNo(), EMAIL).status());
	}

	@Test
	void expiringOrderIsReconciledEvenWhenPaymentIsYoung() {
		OrderView o = newOrder();
		String trade = payments.start(o.orderNo(), EMAIL).tradeNo();
		em.flush();
		jdbc.update("update payment set method = 'ECPAY_CREDIT' where trade_no = ?", trade);
		jdbc.update("update shop_order set created_at = now() - interval '31 minutes' where order_no = ?",
				o.orderNo());
		em.clear();
		answers.put(trade, new PaymentQuery.Info("1", o.totalTwd(), "x", "1111"));
		reconciler.run();
		em.flush();
		em.clear();
		assertEquals(OrderStatus.PAID, orders.get(o.orderNo(), EMAIL).status());
	}

	@Test
	void queryFailureIsSwallowed() {
		OrderView o = newOrder();
		pendingEcpay(o);
		PaymentReconciler broken = new PaymentReconciler(paymentRepo, payments, t -> {
			throw new IllegalStateException("boom");
		}, 5, 30);
		broken.run();
		assertEquals(OrderStatus.PENDING_PAYMENT, orders.get(o.orderNo(), EMAIL).status());
	}

	@Test
	void queryResponseIsParsedAndVerified() {
		EcpayQueryClient client = new EcpayQueryClient(ecpay, "http://127.0.0.1:9");
		Map<String, String> p = new LinkedHashMap<>();
		p.put("MerchantID", "3002607");
		p.put("MerchantTradeNo", "X01");
		p.put("TradeNo", "2610140000009");
		p.put("TradeAmt", "320");
		p.put("TradeStatus", "1");
		p.put("card4no", "4242");
		p.put("CheckMacValue", ecpay.mac(p));
		StringBuilder body = new StringBuilder();
		p.forEach((k, v) -> body.append(body.length() == 0 ? "" : "&").append(k).append('=').append(v));
		Optional<PaymentQuery.Info> ok = client.parse(body.toString());
		assertTrue(ok.isPresent());
		assertEquals(320L, ok.get().amount());
		assertEquals("1", ok.get().status());
		assertTrue(client.parse(body.toString().replace("TradeAmt=320", "TradeAmt=1")).isEmpty());
		assertTrue(client.parse("").isEmpty());
		assertTrue(client.query("X01").isEmpty());
	}
}
