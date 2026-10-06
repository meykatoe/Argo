package com.argo.order;

import static com.argo.order.OrderTestSupport.card;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.argo.card.Card;
import com.argo.card.CardRepository;
import com.argo.card.CardSetRepository;
import com.argo.customer.CustomerAuthService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
@Transactional
class CustomerOrderTests {

	static final String PW = "correct-horse-1";
	static final String BODY = "{\"items\":[{\"cardId\":%d,\"quantity\":1}],"
			+ "\"customer\":{\"name\":\"王小明\",\"email\":\"buyer@test.local\",\"phone\":\"0912345678\"},"
			+ "\"shipping\":{\"recipientName\":\"王小明\",\"recipientPhone\":\"0912345678\",\"postalCode\":\"100\","
			+ "\"city\":\"台北市\",\"address\":\"中正區重慶南路一段 122 號\"}}";

	@Autowired
	CustomerAuthService customers;
	@Autowired
	CardRepository cards;
	@Autowired
	CardSetRepository sets;
	@Autowired
	JdbcTemplate jdbc;
	@Autowired
	WebApplicationContext wac;
	@PersistenceContext
	EntityManager em;

	MockMvc mvc;
	Card a;

	@BeforeEach
	void setUp() {
		mvc = MockMvcBuilders.webAppContextSetup(wac).build();
		a = card(cards, sets, jdbc, "TS-90", "A", 10.0, 50);
		em.clear();
	}

	private ResultActions order(String bearer) throws Exception {
		var req = post("/api/orders").contentType(MediaType.APPLICATION_JSON).content(BODY.formatted(a.getId()));
		if (bearer != null) {
			req.header("Authorization", "Bearer " + bearer);
		}
		return mvc.perform(req);
	}

	private Long customerOf(String orderNo) {
		return jdbc.queryForObject("select customer_id from shop_order where order_no = ?", Long.class, orderNo);
	}

	private String orderNo(ResultActions r) throws Exception {
		String body = r.andReturn().getResponse().getContentAsString();
		return body.replaceAll(".*\"orderNo\":\"([^\"]+)\".*", "$1");
	}

	@Test
	void guestOrderHasNoCustomer() throws Exception {
		String no = orderNo(order(null).andExpect(status().isCreated()));
		assertNull(customerOf(no));
	}

	@Test
	void loggedInOrderIsLinkedToTheAccount() throws Exception {
		var me = customers.register("c1@test.local", PW, null);
		String no = orderNo(order(me.token()).andExpect(status().isCreated()));
		Long id = jdbc.queryForObject("select id from customer_account where email = 'c1@test.local'", Long.class);
		assertEquals(id, customerOf(no));
	}

	@Test
	void staleOrBogusTokenFallsBackToGuest() throws Exception {
		String no = orderNo(order("bogus-token").andExpect(status().isCreated()));
		assertNull(customerOf(no));
	}

	@Test
	void myOrdersListsOnlyMyOwnNewestFirst() throws Exception {
		var me = customers.register("c2@test.local", PW, null);
		var other = customers.register("c3@test.local", PW, null);
		String first = orderNo(order(me.token()));
		String second = orderNo(order(me.token()));
		order(other.token());
		order(null);
		mvc.perform(get("/api/me/orders").header("Authorization", "Bearer " + me.token()))
				.andExpect(status().isOk()).andExpect(jsonPath("$.data.total").value(2))
				.andExpect(jsonPath("$.data.items[0].orderNo").value(second))
				.andExpect(jsonPath("$.data.items[1].orderNo").value(first))
				.andExpect(jsonPath("$.data.items[0].items[0].cardSetId").exists());
		mvc.perform(get("/api/me/orders").header("Authorization", "Bearer " + other.token()))
				.andExpect(jsonPath("$.data.total").value(1));
	}

	@Test
	void myOrdersPaginationIsBounded() throws Exception {
		var me = customers.register("c4@test.local", PW, null);
		mvc.perform(get("/api/me/orders").param("size", "51").header("Authorization", "Bearer " + me.token()))
				.andExpect(status().isBadRequest());
		mvc.perform(get("/api/me/orders").param("page", "0").header("Authorization", "Bearer " + me.token()))
				.andExpect(status().isBadRequest());
		mvc.perform(get("/api/me/orders").header("Authorization", "Bearer " + me.token()))
				.andExpect(jsonPath("$.data.total").value(0));
	}

	@Test
	void guestLookupByEmailStillWorksForLinkedOrders() throws Exception {
		var me = customers.register("c5@test.local", PW, null);
		String no = orderNo(order(me.token()));
		mvc.perform(get("/api/orders/" + no).param("email", "buyer@test.local")).andExpect(status().isOk());
	}

	@Test
	void registeringWithAnOrdersEmailDoesNotClaimOldGuestOrders() throws Exception {
		String no = orderNo(order(null));
		var me = customers.register("buyer@test.local", PW, null);
		assertNull(customerOf(no));
		mvc.perform(get("/api/me/orders").header("Authorization", "Bearer " + me.token()))
				.andExpect(jsonPath("$.data.total").value(0));
	}
}
