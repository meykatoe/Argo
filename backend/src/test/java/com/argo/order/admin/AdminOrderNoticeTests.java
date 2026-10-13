package com.argo.order.admin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.argo.card.Card;
import com.argo.card.CardRepository;
import com.argo.card.CardSet;
import com.argo.card.CardSetRepository;
import com.argo.card.OptcgCard;
import com.argo.order.OrderItem;
import com.argo.order.OrderNotice;
import com.argo.order.ShopOrder;
import com.argo.order.ShopOrderRepository;
import com.argo.staff.auth.LoginPortal;
import com.argo.staff.auth.StaffAuthService;
import com.argo.staff.auth.StaffRole;
import java.math.BigDecimal;
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
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

// 測試交易會回滾，故只驗證事件有送出
@SpringBootTest
@Import(AdminOrderNoticeTests.Config.class)
@Transactional
class AdminOrderNoticeTests {

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
	WebApplicationContext wac;
	@Autowired
	ShopOrderRepository orders;
	@Autowired
	CardRepository cards;
	@Autowired
	CardSetRepository sets;
	@Autowired
	StaffAuthService auth;
	@Autowired
	Recorder recorder;

	MockMvc mvc;
	String hdr;
	Long cardId;

	@BeforeEach
	void setUp() {
		mvc = MockMvcBuilders.webAppContextSetup(wac).build();
		auth.create("noticesvc1", "Password-1234", StaffRole.SERVICE);
		hdr = "Bearer " + auth.login("noticesvc1", "Password-1234", LoginPortal.ADMIN).token();
		sets.save(new CardSet("ZT-10", "Test Set", "booster"));
		Card c = new Card("ZT-10|A|");
		c.fill(new OptcgCard("ZT10-A", "ZT10-A", "ZT-10", "Test Set", "Card A", null, "C", "Red",
				"Character", "1", "1000", null, null, null, null, null, 10.0, 1.0, null),
				new BigDecimal("0.9"));
		cardId = cards.saveAndFlush(c).getId();
		recorder.seen.clear();
	}

	void paidOrder(String no) {
		ShopOrder o = new ShopOrder(no, "USD", "zh-TW");
		o.setAmounts(new BigDecimal("10.00"), new BigDecimal("2.00"));
		o.setCustomer("Zt Buyer", "zt-notice@example.test", "0900000000");
		o.setShipping("Zt Recv", "0911111111", "100", "Taipei", "Road 1");
		o.addItem(new OrderItem(cardId, "ZT10-A", "名", "Name", null, new BigDecimal("5.00"), 2));
		o.markPaid();
		orders.saveAndFlush(o);
	}

	@Test
	void shipPublishesShippedWithTracking() throws Exception {
		paidOrder("ZT-NTC-0001");
		mvc.perform(post("/api/admin/orders/ZT-NTC-0001/ship").header("Authorization", hdr)
				.contentType(MediaType.APPLICATION_JSON).content("{\"trackingNo\":\"TW123\"}"))
				.andExpect(status().isOk());
		assertEquals(1, recorder.seen.size());
		OrderNotice n = recorder.seen.get(0);
		assertEquals(OrderNotice.Kind.SHIPPED, n.kind());
		assertEquals("TW123", n.trackingNo());
		assertEquals("zt-notice@example.test", n.email());
	}

	@Test
	void staffCancelPublishesCancelled() throws Exception {
		paidOrder("ZT-NTC-0002");
		mvc.perform(post("/api/admin/orders/ZT-NTC-0002/cancel").header("Authorization", hdr))
				.andExpect(status().isOk());
		assertEquals(1, recorder.seen.size());
		assertEquals(OrderNotice.Kind.CANCELLED, recorder.seen.get(0).kind());
		assertEquals("STAFF", recorder.seen.get(0).cancelReason());
	}

	@Test
	void rejectedShipPublishesNothing() throws Exception {
		ShopOrder o = new ShopOrder("ZT-NTC-0003", "USD", "en");
		o.setAmounts(new BigDecimal("10.00"), new BigDecimal("2.00"));
		o.setCustomer("Zt Buyer", "zt-notice@example.test", "0900000000");
		o.setShipping("Zt Recv", "0911111111", "100", "Taipei", "Road 1");
		o.addItem(new OrderItem(cardId, "ZT10-A", "名", "Name", null, new BigDecimal("5.00"), 2));
		orders.saveAndFlush(o);
		mvc.perform(post("/api/admin/orders/ZT-NTC-0003/ship").header("Authorization", hdr)
				.contentType(MediaType.APPLICATION_JSON).content("{}"))
				.andExpect(status().isConflict());
		assertEquals(0, recorder.seen.size());
	}
}
