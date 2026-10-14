package com.argo.order;

import static com.argo.order.OrderTestSupport.EMAIL;
import static com.argo.order.OrderTestSupport.card;
import static com.argo.order.OrderTestSupport.request;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.argo.card.Card;
import com.argo.card.CardRepository;
import com.argo.card.CardSetRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
@Transactional
class EcpayCallbackTests {

	@Autowired
	WebApplicationContext wac;
	@Autowired
	OrderService orders;
	@Autowired
	PaymentService payments;
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

	MockMvc mvc;
	Card a;

	@BeforeEach
	void setUp() {
		mvc = MockMvcBuilders.webAppContextSetup(wac).build();
		a = card(cards, sets, jdbc, "TS-51", "A", 10.0, 5);
		em.clear();
	}

	private OrderView newOrder() {
		return orders.create(request(a.getId(), 1L), "en", null);
	}

	private Map<String, String> signed(String trade, long amount, String rtn) {
		Map<String, String> p = new LinkedHashMap<>();
		p.put("MerchantID", "3002607");
		p.put("MerchantTradeNo", trade);
		p.put("RtnCode", rtn);
		p.put("RtnMsg", "ok");
		p.put("TradeNo", "2610140000001");
		p.put("TradeAmt", Long.toString(amount));
		p.put("PaymentType", "Credit_CreditCard");
		p.put("card4no", "2222");
		p.put("CheckMacValue", ecpay.mac(p));
		return p;
	}

	private org.springframework.test.web.servlet.ResultActions send(String path, Map<String, String> p)
			throws Exception {
		var req = post(path).contentType(MediaType.APPLICATION_FORM_URLENCODED);
		p.forEach(req::param);
		return mvc.perform(req);
	}

	@Test
	void validNotifyMarksPaidAndIsIdempotent() throws Exception {
		OrderView o = newOrder();
		String trade = payments.start(o.orderNo(), EMAIL).tradeNo();
		send("/api/payments/ecpay/notify", signed(trade, o.totalTwd(), "1"))
				.andExpect(status().isOk()).andExpect(content().string("1|OK"));
		OrderView paid = orders.get(o.orderNo(), EMAIL);
		assertEquals(OrderStatus.PAID, paid.status());
		assertEquals("2222", paid.payment().cardLast4());
		send("/api/payments/ecpay/notify", signed(trade, o.totalTwd(), "1")).andExpect(content().string("1|OK"));
		assertEquals(1, jdbc.queryForObject(
				"select count(*) from payment where trade_no = ? and status = 'SUCCEEDED'", Integer.class, trade));
	}

	@Test
	void badSignatureIsRejected() throws Exception {
		OrderView o = newOrder();
		String trade = payments.start(o.orderNo(), EMAIL).tradeNo();
		Map<String, String> p = signed(trade, o.totalTwd(), "1");
		p.put("TradeAmt", "1");
		send("/api/payments/ecpay/notify", p).andExpect(content().string("0|REJECTED"));
		assertEquals(OrderStatus.PENDING_PAYMENT, orders.get(o.orderNo(), EMAIL).status());
	}

	@Test
	void wrongAmountIsRejected() throws Exception {
		OrderView o = newOrder();
		String trade = payments.start(o.orderNo(), EMAIL).tradeNo();
		send("/api/payments/ecpay/notify", signed(trade, o.totalTwd() - 1, "1"))
				.andExpect(content().string("0|REJECTED"));
		OrderView after = orders.get(o.orderNo(), EMAIL);
		assertEquals(OrderStatus.PENDING_PAYMENT, after.status());
		assertEquals("AMOUNT_MISMATCH", after.payment().failureCode());
	}

	@Test
	void failureCodeKeepsOrderPending() throws Exception {
		OrderView o = newOrder();
		String trade = payments.start(o.orderNo(), EMAIL).tradeNo();
		send("/api/payments/ecpay/notify", signed(trade, o.totalTwd(), "10100058"))
				.andExpect(content().string("1|OK"));
		OrderView after = orders.get(o.orderNo(), EMAIL);
		assertEquals(OrderStatus.PENDING_PAYMENT, after.status());
		assertEquals("ECPAY_10100058", after.payment().failureCode());
	}

	@Test
	void unknownTradeNoIsRejected() throws Exception {
		send("/api/payments/ecpay/notify", signed("NOPE01", 100, "1")).andExpect(content().string("0|REJECTED"));
	}

	@Test
	void returnPageAppliesResultAndRedirects() throws Exception {
		OrderView o = newOrder();
		String trade = payments.start(o.orderNo(), EMAIL).tradeNo();
		send("/api/payments/ecpay/return", signed(trade, o.totalTwd(), "1"))
				.andExpect(status().isSeeOther())
				.andExpect(header().string("Location", "http://localhost:5173/pay/result?order=" + o.orderNo()));
		assertEquals(OrderStatus.PAID, orders.get(o.orderNo(), EMAIL).status());
	}

	@Test
	void returnWithBadSignatureOnlyRedirects() throws Exception {
		OrderView o = newOrder();
		String trade = payments.start(o.orderNo(), EMAIL).tradeNo();
		Map<String, String> p = signed(trade, o.totalTwd(), "1");
		p.put("CheckMacValue", "BAD");
		send("/api/payments/ecpay/return", p).andExpect(status().isSeeOther())
				.andExpect(header().string("Location", "http://localhost:5173/pay/result"));
		assertEquals(OrderStatus.PENDING_PAYMENT, orders.get(o.orderNo(), EMAIL).status());
	}

	@Test
	void successAfterCancelDoesNotRevive() throws Exception {
		OrderView o = newOrder();
		String trade = payments.start(o.orderNo(), EMAIL).tradeNo();
		orders.cancel(o.orderNo(), EMAIL);
		send("/api/payments/ecpay/notify", signed(trade, o.totalTwd(), "1")).andExpect(content().string("1|OK"));
		OrderView after = orders.get(o.orderNo(), EMAIL);
		assertEquals(OrderStatus.CANCELLED, after.status());
		assertTrue("SUCCEEDED".equals(after.payment().status()));
	}
}
