package com.argo.order;

import static com.argo.order.OrderTestSupport.card;
import static com.argo.order.OrderTestSupport.start;
import static com.argo.order.OrderTestSupport.request;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.argo.card.Card;
import com.argo.card.CardRepository;
import com.argo.card.CardSetRepository;
import com.argo.common.ApiException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

// 測試交易會回滾，故只驗證事件有送出
@SpringBootTest
@Import(OrderNoticeEventTests.Config.class)
@Transactional
class OrderNoticeEventTests {

	static class Recorder {

		final List<OrderNotice> seen = new ArrayList<>();

		@EventListener
		void on(OrderNotice n) {
			seen.add(n);
		}
	}

	@TestConfiguration
	static class Config {

		@Bean
		Recorder recorder() {
			return new Recorder();
		}
	}

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
	Recorder recorder;
	@PersistenceContext
	EntityManager em;

	Card a;

	@BeforeEach
	void setUp() {
		a = card(cards, sets, jdbc, "TS-51", "A", 10.0, 5);
		em.clear();
		recorder.seen.clear();
	}

	List<OrderNotice.Kind> kinds() {
		return recorder.seen.stream().map(OrderNotice::kind).toList();
	}

	@Test
	void createPublishesCreated() {
		OrderView o = orders.create(request(a.getId(), 2L), "zh-TW", null);
		assertEquals(List.of(OrderNotice.Kind.CREATED), kinds());
		OrderNotice n = recorder.seen.get(0);
		assertEquals(o.orderNo(), n.orderNo());
		assertEquals("buyer@test.local", n.email());
		assertEquals(1, n.items().size());
		assertEquals(2, n.items().get(0).quantity());
	}

	@Test
	void successfulPaymentPublishesPaid() {
		OrderView o = orders.create(request(a.getId(), 1L), "en", null);
		payments.completeFake(o.orderNo(), "buyer@test.local", start(payments, o.orderNo()), true);
		assertEquals(List.of(OrderNotice.Kind.CREATED, OrderNotice.Kind.PAID), kinds());
	}

	@Test
	void declinedPaymentPublishesNothingNew() {
		OrderView o = orders.create(request(a.getId(), 1L), "en", null);
		payments.completeFake(o.orderNo(), "buyer@test.local", start(payments, o.orderNo()), false);
		assertEquals(List.of(OrderNotice.Kind.CREATED), kinds());
	}

	@Test
	void cancelPublishesCancelledWithReason() {
		OrderView o = orders.create(request(a.getId(), 1L), "en", null);
		orders.cancel(o.orderNo(), "buyer@test.local");
		OrderNotice n = recorder.seen.get(1);
		assertEquals(OrderNotice.Kind.CANCELLED, n.kind());
		assertEquals("CUSTOMER", n.cancelReason());
	}
}
