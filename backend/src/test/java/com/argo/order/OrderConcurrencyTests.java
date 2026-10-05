package com.argo.order;

import static com.argo.order.OrderTestSupport.card;
import static com.argo.order.OrderTestSupport.request;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.argo.card.Card;
import com.argo.card.CardRepository;
import com.argo.card.CardSetRepository;
import com.argo.common.ApiException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

// 不加交易，用真實提交驗證併發與回滾
@SpringBootTest
class OrderConcurrencyTests {

	@Autowired
	OrderService orders;
	@Autowired
	CardRepository cards;
	@Autowired
	CardSetRepository sets;
	@Autowired
	JdbcTemplate jdbc;

	@AfterEach
	void cleanUp() {
		jdbc.update("delete from payment where order_id in (select id from shop_order where customer_email = 'buyer@test.local')");
		jdbc.update("delete from shop_order_item where order_id in (select id from shop_order where customer_email = 'buyer@test.local')");
		jdbc.update("delete from shop_order where customer_email = 'buyer@test.local'");
		jdbc.update("delete from card where set_id = 'TS-CC'");
		jdbc.update("delete from card_set where set_id = 'TS-CC'");
	}

	private int stock(Card c) {
		return jdbc.queryForObject("select stock from card where id = ?", Integer.class, c.getId());
	}

	@Test
	void neverOversells() throws Exception {
		Card c = card(cards, sets, jdbc, "TS-CC", "HOT", 5.0, 5);
		int threads = 12;
		ExecutorService pool = Executors.newFixedThreadPool(threads);
		CountDownLatch start = new CountDownLatch(1);
		List<Future<Boolean>> results = new ArrayList<>();
		for (int i = 0; i < threads; i++) {
			results.add(pool.submit(() -> {
				start.await();
				try {
					orders.create(request(c.getId(), 1L), "en");
					return true;
				} catch (ApiException e) {
					return false;
				}
			}));
		}
		start.countDown();
		int success = 0;
		for (Future<Boolean> f : results) {
			if (f.get()) {
				success++;
			}
		}
		pool.shutdown();
		assertEquals(5, success);
		assertEquals(0, stock(c));
		assertEquals(5, jdbc.queryForObject("select count(*) from shop_order", Integer.class));
	}

	@Test
	void failedOrderRollsBackEarlierDeductions() {
		Card ok = card(cards, sets, jdbc, "TS-CC", "OK", 5.0, 3);
		Card empty = card(cards, sets, jdbc, "TS-CC", "EMPTY", 5.0, 0);
		assertThrows(ApiException.class, () -> orders.create(request(ok.getId(), 1L, empty.getId(), 1L), "en"));
		assertEquals(3, stock(ok));
		assertEquals(0, jdbc.queryForObject(
				"select count(*) from shop_order where customer_email = 'buyer@test.local'", Integer.class));
	}
}
