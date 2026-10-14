package com.argo.order;

import static com.argo.order.OrderTestSupport.EMAIL;
import static com.argo.order.OrderTestSupport.card;
import static com.argo.order.OrderTestSupport.start;
import static com.argo.order.OrderTestSupport.request;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.argo.card.Card;
import com.argo.card.CardRepository;
import com.argo.card.CardSetRepository;
import com.argo.common.ApiException;
import com.argo.card.i18n.TcPageParser;
import com.argo.card.i18n.TranslationSyncService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class OrderFlowTests {

	@Autowired
	OrderService orders;
	@Autowired
	PaymentService payments;
	@Autowired
	CardRepository cards;
	@Autowired
	CardSetRepository sets;
	@Autowired
	JdbcTemplate jdbc;
	@Autowired
	TranslationSyncService translationSync;
	@PersistenceContext
	EntityManager em;

	Card a;
	Card b;

	@BeforeEach
	void setUp() {
		a = card(cards, sets, jdbc, "TS-50", "A", 10.0, 5);
		b = card(cards, sets, jdbc, "TS-50", "B", 2.0, 0);
		em.clear();
	}

	private int stock(Card c) {
		return jdbc.queryForObject("select stock from card where id = ?", Integer.class, c.getId());
	}

	private String code(Runnable r) {
		return assertThrows(ApiException.class, r::run).getCode();
	}

	@Test
	void createDeductsStockAndSnapshots() {
		OrderView o = orders.create(request(a.getId(), 2L), "en", null);
		assertTrue(o.orderNo().matches("^AR\\d{6}-[A-Z2-9]{6}$"));
		assertEquals(OrderStatus.PENDING_PAYMENT, o.status());
		assertEquals(3, stock(a));
		assertEquals(0, new BigDecimal("18.00").compareTo(o.subtotal()));
		assertEquals(0, new BigDecimal("18.00").compareTo(o.total()));
		assertEquals(1, o.items().size());
		assertEquals("Card A", o.items().get(0).cardName());
		assertEquals(0, new BigDecimal("9.00").compareTo(o.items().get(0).unitPrice()));
		assertEquals("buyer@test.local", o.customerEmail());
	}

	@Test
	void duplicateLinesMerge() {
		OrderView o = orders.create(request(a.getId(), 1L, a.getId(), 2L), "en", null);
		assertEquals(1, o.items().size());
		assertEquals(3, o.items().get(0).quantity());
		assertEquals(2, stock(a));
	}

	@Test
	void rejectsBadItems() {
		assertEquals("ITEM_UNAVAILABLE", code(() -> orders.create(request(b.getId(), 1L), "en", null)));
		assertEquals("INSUFFICIENT_STOCK", code(() -> orders.create(request(a.getId(), 6L), "en", null)));
		assertEquals("ITEM_NOT_FOUND", code(() -> orders.create(request(999999999L, 1L), "en", null)));
		assertEquals("INVALID_QUANTITY",
				code(() -> orders.create(request(a.getId(), 60L, a.getId(), 60L), "en", null)));
		assertEquals(5, stock(a));
	}

	@Test
	void usesTranslatedNameWhenAvailable() {
		translationSync.saveCards(java.util.List.of(
				new TcPageParser.Card("TS50-A", "測試卡A", null, null)));
		OrderView o = orders.create(request(a.getId(), 1L), "zh-TW", null);
		assertEquals("測試卡A", o.items().get(0).cardName());
		assertEquals("Card A", o.items().get(0).cardNameEn());
	}

	@Test
	void lookupNeedsMatchingEmail() {
		OrderView o = orders.create(request(a.getId(), 1L), "en", null);
		assertEquals(o.orderNo(), orders.get(o.orderNo(), "BUYER@test.local").orderNo());
		assertEquals("ORDER_NOT_FOUND", code(() -> orders.get(o.orderNo(), "other@test.local")));
		assertEquals("ORDER_NOT_FOUND", code(() -> orders.get("AR000000-AAAAAA", EMAIL)));
	}

	@Test
	void startRecordsPendingPaymentInTwd() {
		OrderView o = orders.create(request(a.getId(), 1L), "en", null);
		assertNotNull(o.totalTwd());
		assertEquals(o.totalTwd(), Math.round(o.total().doubleValue() * o.fxRate().doubleValue()));
		PayStart s = payments.start(o.orderNo(), EMAIL);
		assertEquals("FAKE", s.mode());
		assertTrue(s.tradeNo().matches("^[A-Za-z0-9]{1,20}$"));
		assertEquals("PENDING", orders.get(o.orderNo(), EMAIL).payment().status());
		assertEquals(OrderStatus.PENDING_PAYMENT, orders.get(o.orderNo(), EMAIL).status());
		// 每次嘗試的交易編號不同
		assertTrue(!s.tradeNo().equals(payments.start(o.orderNo(), EMAIL).tradeNo()));
	}

	@Test
	void fakeSuccessMarksPaid() {
		OrderView o = orders.create(request(a.getId(), 1L), "en", null);
		OrderView paid = payments.completeFake(o.orderNo(), EMAIL, start(payments, o.orderNo()), true);
		assertEquals(OrderStatus.PAID, paid.status());
		assertNotNull(paid.paidAt());
		assertEquals("SUCCEEDED", paid.payment().status());
	}

	@Test
	void fakeFailureKeepsOrderPendingAndAllowsRetry() {
		OrderView o = orders.create(request(a.getId(), 1L), "en", null);
		OrderView failed = payments.completeFake(o.orderNo(), EMAIL, start(payments, o.orderNo()), false);
		assertEquals(OrderStatus.PENDING_PAYMENT, failed.status());
		assertEquals("FAILED", failed.payment().status());
		assertEquals("FAKE_DECLINED", failed.payment().failureCode());
		assertEquals(OrderStatus.PAID,
				payments.completeFake(o.orderNo(), EMAIL, start(payments, o.orderNo()), true).status());
	}

	@Test
	void repeatedCompletionIsIgnored() {
		OrderView o = orders.create(request(a.getId(), 1L), "en", null);
		String trade = start(payments, o.orderNo());
		payments.completeFake(o.orderNo(), EMAIL, trade, true);
		OrderView again = payments.completeFake(o.orderNo(), EMAIL, trade, false);
		assertEquals(OrderStatus.PAID, again.status());
		assertEquals("SUCCEEDED", again.payment().status());
	}

	@Test
	void startChecksEmailAndState() {
		OrderView o = orders.create(request(a.getId(), 1L), "en", null);
		assertEquals("ORDER_NOT_FOUND", code(() -> payments.start(o.orderNo(), "x@test.local")));
		assertNull(orders.get(o.orderNo(), EMAIL).payment());
		payments.completeFake(o.orderNo(), EMAIL, start(payments, o.orderNo()), true);
		assertEquals("ORDER_NOT_PAYABLE", code(() -> payments.start(o.orderNo(), EMAIL)));
		assertEquals("ORDER_NOT_CANCELLABLE", code(() -> orders.cancel(o.orderNo(), EMAIL)));
	}

	@Test
	void fakeCompleteNeedsOwnTradeNo() {
		OrderView o1 = orders.create(request(a.getId(), 1L), "en", null);
		OrderView o2 = orders.create(request(a.getId(), 1L), "en", null);
		String trade1 = start(payments, o1.orderNo());
		assertEquals("ORDER_NOT_FOUND", code(() -> payments.completeFake(o2.orderNo(), EMAIL, trade1, true)));
		assertEquals("ORDER_NOT_FOUND", code(() -> payments.completeFake(o1.orderNo(), EMAIL, "nope", true)));
	}

	@Test
	void cancelRestoresStock() {
		OrderView o = orders.create(request(a.getId(), 3L), "en", null);
		assertEquals(2, stock(a));
		OrderView c = orders.cancel(o.orderNo(), EMAIL);
		assertEquals(OrderStatus.CANCELLED, c.status());
		assertEquals("CUSTOMER", c.cancelReason());
		assertEquals(5, stock(a));
		assertEquals("ORDER_NOT_PAYABLE", code(() -> payments.start(o.orderNo(), EMAIL)));
		assertEquals("ORDER_NOT_CANCELLABLE", code(() -> orders.cancel(o.orderNo(), EMAIL)));
	}

	@Test
	void expireOverdueRestoresStock() {
		OrderView o = orders.create(request(a.getId(), 2L), "en", null);
		jdbc.update("update shop_order set created_at = now() - interval '2 hours' where order_no = ?", o.orderNo());
		em.clear();
		assertTrue(orders.expireOverdue() >= 1);
		OrderView after = orders.get(o.orderNo(), EMAIL);
		assertEquals(OrderStatus.CANCELLED, after.status());
		assertEquals("EXPIRED", after.cancelReason());
		assertEquals(5, stock(a));
	}

	@Test
	void payingExpiredOrderCancelsIt() {
		OrderView o = orders.create(request(a.getId(), 2L), "en", null);
		jdbc.update("update shop_order set created_at = now() - interval '2 hours' where order_no = ?", o.orderNo());
		em.clear();
		assertEquals("ORDER_EXPIRED", code(() -> payments.start(o.orderNo(), EMAIL)));
		assertEquals(OrderStatus.CANCELLED, orders.get(o.orderNo(), EMAIL).status());
		assertEquals(5, stock(a));
	}
}
