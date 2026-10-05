package com.argo.order;

import static com.argo.order.OrderTestSupport.EMAIL;
import static com.argo.order.OrderTestSupport.card;
import static com.argo.order.OrderTestSupport.pay;
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
import com.argo.i18n.TcPageParser;
import com.argo.i18n.TranslationSyncService;
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
		OrderView o = orders.create(request(a.getId(), 2L), "en");
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
		OrderView o = orders.create(request(a.getId(), 1L, a.getId(), 2L), "en");
		assertEquals(1, o.items().size());
		assertEquals(3, o.items().get(0).quantity());
		assertEquals(2, stock(a));
	}

	@Test
	void rejectsBadItems() {
		assertEquals("ITEM_UNAVAILABLE", code(() -> orders.create(request(b.getId(), 1L), "en")));
		assertEquals("INSUFFICIENT_STOCK", code(() -> orders.create(request(a.getId(), 6L), "en")));
		assertEquals("ITEM_NOT_FOUND", code(() -> orders.create(request(999999999L, 1L), "en")));
		assertEquals("INVALID_QUANTITY",
				code(() -> orders.create(request(a.getId(), 60L, a.getId(), 60L), "en")));
		assertEquals(5, stock(a));
	}

	@Test
	void usesTranslatedNameWhenAvailable() {
		translationSync.saveCards(java.util.List.of(
				new TcPageParser.Card("TS50-A", "測試卡A", null, null)));
		OrderView o = orders.create(request(a.getId(), 1L), "zh-TW");
		assertEquals("測試卡A", o.items().get(0).cardName());
		assertEquals("Card A", o.items().get(0).cardNameEn());
	}

	@Test
	void lookupNeedsMatchingEmail() {
		OrderView o = orders.create(request(a.getId(), 1L), "en");
		assertEquals(o.orderNo(), orders.get(o.orderNo(), "BUYER@test.local").orderNo());
		assertEquals("ORDER_NOT_FOUND", code(() -> orders.get(o.orderNo(), "other@test.local")));
		assertEquals("ORDER_NOT_FOUND", code(() -> orders.get("AR000000-AAAAAA", EMAIL)));
	}

	@Test
	void payWithGoodCard() {
		OrderView o = orders.create(request(a.getId(), 1L), "en");
		OrderView paid = payments.pay(o.orderNo(), pay("4242 4242 4242 4242"));
		assertEquals(OrderStatus.PAID, paid.status());
		assertNotNull(paid.paidAt());
		assertEquals("SUCCEEDED", paid.payment().status());
		assertEquals("4242", paid.payment().cardLast4());
		// 資料庫不應有完整卡號
		Integer leaked = jdbc.queryForObject(
				"select count(*) from payment where card_last4 <> '4242' or length(card_last4) <> 4"
						+ " and order_id = (select id from shop_order where order_no = ?)",
				Integer.class, o.orderNo());
		assertEquals(0, leaked);
	}

	@Test
	void declinedCardKeepsOrderPendingAndAllowsRetry() {
		OrderView o = orders.create(request(a.getId(), 1L), "en");
		assertEquals("CARD_DECLINED", code(() -> payments.pay(o.orderNo(), pay("4000000000000002"))));
		OrderView after = orders.get(o.orderNo(), EMAIL);
		assertEquals(OrderStatus.PENDING_PAYMENT, after.status());
		assertEquals("FAILED", after.payment().status());
		assertEquals("CARD_DECLINED", after.payment().failureCode());
		assertEquals(OrderStatus.PAID, payments.pay(o.orderNo(), pay("4242424242424242")).status());
	}

	@Test
	void otherFailureCodes() {
		OrderView o = orders.create(request(a.getId(), 1L), "en");
		assertEquals("INSUFFICIENT_FUNDS", code(() -> payments.pay(o.orderNo(), pay("4000000000009995"))));
		assertEquals("PROCESSING_ERROR", code(() -> payments.pay(o.orderNo(), pay("4000000000000119"))));
	}

	@Test
	void rejectsBadCards() {
		OrderView o = orders.create(request(a.getId(), 1L), "en");
		assertEquals("INVALID_CARD", code(() -> payments.pay(o.orderNo(), pay("4242424242424241"))));
		assertEquals("INVALID_CARD", code(() -> payments.pay(o.orderNo(), pay("1234"))));
		var expired = new PayRequest(EMAIL, new PayRequest.Card("4242424242424242", 1, 2020, "123", "W"));
		assertEquals("CARD_EXPIRED", code(() -> payments.pay(o.orderNo(), expired)));
		var wrongMail = new PayRequest("x@test.local", new PayRequest.Card("4242424242424242", 12, 2099, "123", "W"));
		assertEquals("ORDER_NOT_FOUND", code(() -> payments.pay(o.orderNo(), wrongMail)));
		assertNull(orders.get(o.orderNo(), EMAIL).payment());
	}

	@Test
	void cannotPayTwice() {
		OrderView o = orders.create(request(a.getId(), 1L), "en");
		payments.pay(o.orderNo(), pay("4242424242424242"));
		assertEquals("ORDER_NOT_PAYABLE", code(() -> payments.pay(o.orderNo(), pay("4242424242424242"))));
		assertEquals("ORDER_NOT_CANCELLABLE", code(() -> orders.cancel(o.orderNo(), EMAIL)));
	}

	@Test
	void cancelRestoresStock() {
		OrderView o = orders.create(request(a.getId(), 3L), "en");
		assertEquals(2, stock(a));
		OrderView c = orders.cancel(o.orderNo(), EMAIL);
		assertEquals(OrderStatus.CANCELLED, c.status());
		assertEquals("CUSTOMER", c.cancelReason());
		assertEquals(5, stock(a));
		assertEquals("ORDER_NOT_PAYABLE", code(() -> payments.pay(o.orderNo(), pay("4242424242424242"))));
		assertEquals("ORDER_NOT_CANCELLABLE", code(() -> orders.cancel(o.orderNo(), EMAIL)));
	}

	@Test
	void expireOverdueRestoresStock() {
		OrderView o = orders.create(request(a.getId(), 2L), "en");
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
		OrderView o = orders.create(request(a.getId(), 2L), "en");
		jdbc.update("update shop_order set created_at = now() - interval '2 hours' where order_no = ?", o.orderNo());
		em.clear();
		assertEquals("ORDER_EXPIRED", code(() -> payments.pay(o.orderNo(), pay("4242424242424242"))));
		assertEquals(OrderStatus.CANCELLED, orders.get(o.orderNo(), EMAIL).status());
		assertEquals(5, stock(a));
	}
}
