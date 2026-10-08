package com.argo.order.admin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.argo.card.Card;
import com.argo.card.CardRepository;
import com.argo.card.CardSet;
import com.argo.card.CardSetRepository;
import com.argo.card.OptcgCard;
import com.argo.order.OrderItem;
import com.argo.order.ShopOrder;
import com.argo.order.ShopOrderRepository;
import com.argo.staff.auth.LoginPortal;
import com.argo.staff.auth.StaffAuthService;
import com.argo.staff.auth.StaffRole;
import java.math.BigDecimal;
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
class AdminOrderActionTests {

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
	JdbcTemplate jdbc;

	MockMvc mvc;
	String hdr;
	Long cardId;

	@BeforeEach
	void setUp() {
		mvc = MockMvcBuilders.webAppContextSetup(wac).build();
		auth.create("actsvc1", "password-1234", StaffRole.SERVICE);
		hdr = "Bearer " + auth.login("actsvc1", "password-1234", LoginPortal.ADMIN).token();
		sets.save(new CardSet("ZT-09", "Test Set", "booster"));
		Card c = new Card("ZT-09|A|");
		c.fill(new OptcgCard("ZT09-A", "ZT09-A", "ZT-09", "Test Set", "Card A", null, "C", "Red",
				"Character", "1", "1000", null, null, null, null, null, 10.0, 1.0, null),
				new BigDecimal("0.9"));
		cardId = cards.saveAndFlush(c).getId();
	}

	ShopOrder order(String no) {
		ShopOrder o = new ShopOrder(no, "USD", "zh-TW");
		o.setAmounts(new BigDecimal("10.00"), new BigDecimal("2.00"));
		o.setCustomer("Zt Buyer", "zt-act@example.test", "0900000000");
		o.setShipping("Zt Recv", "0911111111", "100", "Taipei", "Road 1");
		o.addItem(new OrderItem(cardId, "ZT09-A", "名", "Name", null, new BigDecimal("5.00"), 3));
		return orders.saveAndFlush(o);
	}

	int audits(String action, String no) {
		return jdbc.queryForObject("select count(*) from staff_audit_log where action = ? and target_id = ?",
				Integer.class, action, no);
	}

	int stock() {
		return jdbc.queryForObject("select stock from card where id = ?", Integer.class, cardId);
	}

	@Test
	void shipsThenCompletes() throws Exception {
		order("ZT-ACT-0001").markPaid();
		orders.flush();
		mvc.perform(post("/api/admin/orders/ZT-ACT-0001/ship").header("Authorization", hdr)
				.contentType(MediaType.APPLICATION_JSON).content("{\"trackingNo\":\" TW123 \"}"))
				.andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("SHIPPED"))
				.andExpect(jsonPath("$.data.trackingNo").value("TW123"));
		assertEquals(1, audits("ORDER_SHIPPED", "ZT-ACT-0001"));
		mvc.perform(post("/api/admin/orders/ZT-ACT-0001/complete").header("Authorization", hdr))
				.andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("COMPLETED"));
		assertEquals(1, audits("ORDER_COMPLETED", "ZT-ACT-0001"));
	}

	@Test
	void cannotShipUnpaidOrCompleteUnshipped() throws Exception {
		order("ZT-ACT-0002");
		mvc.perform(post("/api/admin/orders/ZT-ACT-0002/ship").header("Authorization", hdr)
				.contentType(MediaType.APPLICATION_JSON).content("{}"))
				.andExpect(status().isConflict()).andExpect(jsonPath("$.msg").value("ORDER_STATE_CONFLICT"));
		mvc.perform(post("/api/admin/orders/ZT-ACT-0002/complete").header("Authorization", hdr))
				.andExpect(status().isConflict());
		assertEquals(0, audits("ORDER_SHIPPED", "ZT-ACT-0002"));
	}

	@Test
	void cancelReturnsStock() throws Exception {
		order("ZT-ACT-0003").markPaid();
		orders.flush();
		int before = stock();
		mvc.perform(post("/api/admin/orders/ZT-ACT-0003/cancel").header("Authorization", hdr))
				.andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("CANCELLED"))
				.andExpect(jsonPath("$.data.cancelReason").value("STAFF"));
		assertEquals(before + 3, stock());
		assertEquals(1, audits("ORDER_CANCELLED", "ZT-ACT-0003"));
		mvc.perform(post("/api/admin/orders/ZT-ACT-0003/cancel").header("Authorization", hdr))
				.andExpect(status().isConflict());
		assertEquals(before + 3, stock());
	}

	@Test
	void shippedOrderCannotBeCancelled() throws Exception {
		ShopOrder o = order("ZT-ACT-0004");
		o.markPaid();
		o.markShipped(null);
		orders.flush();
		mvc.perform(post("/api/admin/orders/ZT-ACT-0004/cancel").header("Authorization", hdr))
				.andExpect(status().isConflict());
	}

	@Test
	void savesNoteWithoutLoggingContent() throws Exception {
		order("ZT-ACT-0005");
		mvc.perform(patch("/api/admin/orders/ZT-ACT-0005/note").header("Authorization", hdr)
				.contentType(MediaType.APPLICATION_JSON).content("{\"note\":\"secret-note\"}"))
				.andExpect(status().isOk()).andExpect(jsonPath("$.data.staffNote").value("secret-note"));
		int leaked = jdbc.queryForObject(
				"select count(*) from staff_audit_log where target_id = 'ZT-ACT-0005' and detail::text like '%secret-note%'",
				Integer.class);
		assertEquals(0, leaked);
		assertEquals(1, audits("ORDER_NOTE_UPDATED", "ZT-ACT-0005"));
		mvc.perform(patch("/api/admin/orders/ZT-ACT-0005/note").header("Authorization", hdr)
				.contentType(MediaType.APPLICATION_JSON).content("{\"note\":\"" + "x".repeat(501) + "\"}"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void unknownOrderIs404() throws Exception {
		mvc.perform(post("/api/admin/orders/ZT-NONE/complete").header("Authorization", hdr))
				.andExpect(status().isNotFound());
	}
}
