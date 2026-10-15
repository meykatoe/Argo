package com.argo.order.admin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.argo.order.OrderItem;
import com.argo.order.Payment;
import com.argo.order.PaymentRepository;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
@Transactional
class AdminRefundTests {

	@Autowired
	WebApplicationContext wac;
	@Autowired
	ShopOrderRepository orders;
	@Autowired
	PaymentRepository payments;
	@Autowired
	StaffAuthService auth;
	@Autowired
	JdbcTemplate jdbc;

	MockMvc mvc;
	String ops;
	String general;

	@BeforeEach
	void setUp() {
		mvc = MockMvcBuilders.webAppContextSetup(wac).build();
		auth.create("rfops1", "Password-1234", StaffRole.OPS);
		auth.create("rfgen1", "Password-1234", StaffRole.GENERAL);
		ops = "Bearer " + auth.login("rfops1", "Password-1234", LoginPortal.OPS).token();
		general = "Bearer " + auth.login("rfgen1", "Password-1234", LoginPortal.ADMIN).token();
	}

	private Payment payment(String no, String trade, boolean refund) {
		ShopOrder o = new ShopOrder(no, "USD", "zh-TW");
		o.setAmounts(new BigDecimal("10.00"), new BigDecimal("2.00"));
		o.setCustomer("Rf Buyer", "rf@example.test", "0900000000");
		o.setShipping("Rf Recv", "0911111111", "100", "Taipei", "Road 1");
		o = orders.saveAndFlush(o);
		Payment p = Payment.pending(o.getId(), "ECPAY_CREDIT", trade, new BigDecimal("288"), "TWD");
		p.succeed("gw-" + trade, "2222");
		if (refund) {
			p.requireRefund();
		}
		return payments.saveAndFlush(p);
	}

	private int audits(Long id) {
		return jdbc.queryForObject(
				"select count(*) from staff_audit_log where action = 'REFUND_MARKED' and target_id = ?",
				Integer.class, String.valueOf(id));
	}

	@Test
	void listShowsOnlyRequiredByDefault() throws Exception {
		Payment need = payment("AR-RFT-0001", "ARRFT000101", true);
		payment("AR-RFT-0002", "ARRFT000201", false);
		mvc.perform(get("/api/ops/refunds?size=50").header("Authorization", ops))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.items[?(@.id == " + need.getId() + ")].orderNo")
						.value("AR-RFT-0001"))
				.andExpect(jsonPath("$.data.items[?(@.tradeNo == 'ARRFT000201')]").isEmpty());
	}

	@Test
	void markRefundedIsAuditedOnce() throws Exception {
		Payment p = payment("AR-RFT-0003", "ARRFT000301", true);
		for (int i = 0; i < 2; i++) {
			mvc.perform(post("/api/ops/refunds/" + p.getId() + "/refunded").header("Authorization", ops))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.data.refundStatus").value("REFUNDED"));
		}
		assertEquals(1, audits(p.getId()));
	}

	@Test
	void notRequiredPaymentIsRefused() throws Exception {
		Payment p = payment("AR-RFT-0004", "ARRFT000401", false);
		mvc.perform(post("/api/ops/refunds/" + p.getId() + "/refunded").header("Authorization", ops))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value(5008));
		assertEquals(0, audits(p.getId()));
	}

	@Test
	void unknownPaymentIsNotFound() throws Exception {
		mvc.perform(post("/api/ops/refunds/999999999/refunded").header("Authorization", ops))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value(5007));
	}

	@Test
	void badStatusIsRejected() throws Exception {
		mvc.perform(get("/api/ops/refunds?status=NONE").header("Authorization", ops))
				.andExpect(status().isBadRequest());
	}

	@Test
	void businessStaffCannotUse() throws Exception {
		mvc.perform(get("/api/ops/refunds").header("Authorization", general))
				.andExpect(status().is4xxClientError());
	}
}
