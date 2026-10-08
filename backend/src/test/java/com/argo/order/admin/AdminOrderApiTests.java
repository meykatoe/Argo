package com.argo.order.admin;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.argo.order.OrderItem;
import com.argo.order.OrderStatus;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
@Transactional
class AdminOrderApiTests {

	@Autowired
	WebApplicationContext wac;
	@Autowired
	ShopOrderRepository orders;
	@Autowired
	StaffAuthService auth;

	MockMvc mvc;
	String service;
	String ops;

	@BeforeEach
	void setUp() {
		mvc = MockMvcBuilders.webAppContextSetup(wac).build();
		auth.create("ordsvc1", "Password-1234", StaffRole.SERVICE);
		auth.create("ordops1", "Password-1234", StaffRole.OPS);
		service = auth.login("ordsvc1", "Password-1234", LoginPortal.ADMIN).token();
		ops = auth.login("ordops1", "Password-1234", LoginPortal.OPS).token();
		save("ZT-ORD-0001", "zt-buyer-one@example.test");
		ShopOrder paid = save("ZT-ORD-0002", "zt-buyer-two@example.test");
		paid.markPaid();
	}

	ShopOrder save(String no, String email) {
		ShopOrder o = new ShopOrder(no, "USD", "zh-TW");
		o.setAmounts(new BigDecimal("10.00"), new BigDecimal("2.00"));
		o.setCustomer("Zt Buyer", email, "0900000000");
		o.setShipping("Zt Recv", "0911111111", "100", "Taipei", "Road 1");
		o.addItem(new OrderItem(1L, "ZT-01", "名", "Name", null, new BigDecimal("5.00"), 2));
		return orders.saveAndFlush(o);
	}

	@Test
	void rejectsMissingToken() throws Exception {
		mvc.perform(get("/api/admin/orders")).andExpect(status().isUnauthorized());
	}

	@Test
	void opsIsForbidden() throws Exception {
		mvc.perform(get("/api/admin/orders").header("Authorization", "Bearer " + ops))
				.andExpect(status().isForbidden());
	}

	@Test
	void listsByKeyword() throws Exception {
		mvc.perform(get("/api/admin/orders").param("keyword", "zt-buyer-one@")
				.header("Authorization", "Bearer " + service)).andExpect(status().isOk())
				.andExpect(jsonPath("$.data.total").value(1))
				.andExpect(jsonPath("$.data.items[0].orderNo").value("ZT-ORD-0001"))
				.andExpect(jsonPath("$.data.items[0].itemCount").value(2));
	}

	@Test
	void filtersByStatus() throws Exception {
		mvc.perform(get("/api/admin/orders").param("keyword", "zt-ord-").param("status", OrderStatus.PAID.name())
				.header("Authorization", "Bearer " + service)).andExpect(status().isOk())
				.andExpect(jsonPath("$.data.total").value(1))
				.andExpect(jsonPath("$.data.items[0].orderNo").value("ZT-ORD-0002"));
	}

	@Test
	void rejectsUnknownStatus() throws Exception {
		mvc.perform(get("/api/admin/orders").param("status", "NOPE")
				.header("Authorization", "Bearer " + service)).andExpect(status().isBadRequest());
	}

	@Test
	void showsDetail() throws Exception {
		mvc.perform(get("/api/admin/orders/ZT-ORD-0001").header("Authorization", "Bearer " + service))
				.andExpect(status().isOk()).andExpect(jsonPath("$.data.customerPhone").value("0900000000"))
				.andExpect(jsonPath("$.data.items[0].quantity").value(2))
				.andExpect(jsonPath("$.data.total").value(12.0));
	}

	@Test
	void unknownOrderIs404() throws Exception {
		mvc.perform(get("/api/admin/orders/ZT-NONE").header("Authorization", "Bearer " + service))
				.andExpect(status().isNotFound()).andExpect(jsonPath("$.msg").value("ORDER_NOT_FOUND"));
	}
}
