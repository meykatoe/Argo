package com.argo.order;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.argo.common.mail.Mailer;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class OrderNotifierTests {

	record Sent(String to, String subject, String body) {
	}

	final List<Sent> sent = new ArrayList<>();
	final OrderNotifier notifier = new OrderNotifier((to, s, b) -> sent.add(new Sent(to, s, b)),
			"http://site.test", 30);

	OrderNotice notice(OrderNotice.Kind kind, String lang, String tracking, String reason) {
		return new OrderNotice(kind, "AR-1", "a@test.local", "Ann", lang, "USD", new BigDecimal("2"),
				new BigDecimal("12.5"), List.of(new OrderNotice.Line("Luffy", 2, new BigDecimal("10.5"))),
				tracking, reason);
	}

	@Test
	void createdMailInChinese() {
		notifier.onNotice(notice(OrderNotice.Kind.CREATED, "zh-TW", null, null));
		Sent m = sent.get(0);
		assertEquals("a@test.local", m.to());
		assertEquals("Argo 訂單 AR-1 已成立", m.subject());
		assertTrue(m.body().contains("30 分鐘內完成付款"));
		assertTrue(m.body().contains("- Luffy x 2  USD 10.50"));
		assertTrue(m.body().contains("總計：USD 12.50"));
		assertTrue(m.body().contains("http://site.test/orders"));
	}

	@Test
	void paidMailInEnglish() {
		notifier.onNotice(notice(OrderNotice.Kind.PAID, "en", null, null));
		assertEquals("Argo order AR-1 payment received", sent.get(0).subject());
		assertTrue(sent.get(0).body().contains("Total: USD 12.50"));
	}

	@Test
	void shippedMailShowsTrackingOnlyWhenPresent() {
		notifier.onNotice(notice(OrderNotice.Kind.SHIPPED, "en", "TW123", null));
		notifier.onNotice(notice(OrderNotice.Kind.SHIPPED, "en", null, null));
		assertTrue(sent.get(0).body().contains("Tracking number: TW123"));
		assertFalse(sent.get(1).body().contains("Tracking"));
	}

	@Test
	void cancelledMailGivesTheReason() {
		notifier.onNotice(notice(OrderNotice.Kind.CANCELLED, "zh-TW", null, "EXPIRED"));
		notifier.onNotice(notice(OrderNotice.Kind.CANCELLED, "zh-TW", null, "CUSTOMER"));
		notifier.onNotice(notice(OrderNotice.Kind.CANCELLED, "en", null, "STAFF"));
		assertTrue(sent.get(0).body().contains("逾時未付款"));
		assertTrue(sent.get(1).body().contains("依您的要求取消"));
		assertTrue(sent.get(2).body().contains("cancelled by the shop"));
	}

	@Test
	void mailFailureDoesNotPropagate() {
		Mailer broken = (to, s, b) -> {
			throw new IllegalStateException("smtp down");
		};
		OrderNotifier n = new OrderNotifier(broken, "http://site.test", 30);
		assertDoesNotThrow(() -> n.onNotice(notice(OrderNotice.Kind.PAID, "en", null, null)));
	}
}
