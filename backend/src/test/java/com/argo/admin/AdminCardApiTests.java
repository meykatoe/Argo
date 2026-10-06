package com.argo.admin;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.argo.card.Card;
import com.argo.card.CardRepository;
import com.argo.card.CardSet;
import com.argo.card.CardSetRepository;
import com.argo.card.OptcgCard;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
@Transactional
@TestPropertySource(properties = "argo.admin.token=secret")
class AdminCardApiTests {

	@Autowired
	WebApplicationContext wac;
	@Autowired
	CardRepository cards;
	@Autowired
	CardSetRepository sets;

	MockMvc mvc;
	Long id;

	@BeforeEach
	void setUp() {
		mvc = MockMvcBuilders.webAppContextSetup(wac).build();
		sets.save(new CardSet("TS-08", "Test Set", "booster"));
		Card c = new Card("TS-08|A|");
		c.fill(new OptcgCard("TS08-A", "TS08-A", "TS-08", "Test Set", "Card A", null, "C", "Red",
				"Character", "1", "1000", null, null, null, null, null, 10.0, 1.0, null),
				new BigDecimal("0.9"));
		id = cards.saveAndFlush(c).getId();
	}

	@Test
	void rejectsMissingToken() throws Exception {
		mvc.perform(get("/api/admin/cards")).andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("ADMIN_UNAUTHORIZED"));
	}

	@Test
	void rejectsWrongToken() throws Exception {
		mvc.perform(patch("/api/admin/cards/" + id + "/extra-discount")
				.header("X-Admin-Token", "nope").contentType(MediaType.APPLICATION_JSON)
				.content("{\"extraDiscount\":0.4}")).andExpect(status().isUnauthorized());
	}

	@Test
	void setsExtraDiscount() throws Exception {
		mvc.perform(patch("/api/admin/cards/" + id + "/extra-discount")
				.header("X-Admin-Token", "secret").contentType(MediaType.APPLICATION_JSON)
				.content("{\"extraDiscount\":0.4}")).andExpect(status().isOk())
				.andExpect(jsonPath("$.listPrice").value(9.0))
				.andExpect(jsonPath("$.extraDiscount").value(0.4))
				.andExpect(jsonPath("$.salePrice").value(3.6));
	}

	@Test
	void rejectsOutOfRange() throws Exception {
		for (String v : new String[] { "0", "1.5", "-1", "0.00001", "null" }) {
			mvc.perform(patch("/api/admin/cards/" + id + "/extra-discount")
					.header("X-Admin-Token", "secret").contentType(MediaType.APPLICATION_JSON)
					.content("{\"extraDiscount\":" + v + "}")).andExpect(status().isBadRequest());
		}
	}

	@Test
	void unknownCardIs404() throws Exception {
		mvc.perform(patch("/api/admin/cards/999999999/extra-discount")
				.header("X-Admin-Token", "secret").contentType(MediaType.APPLICATION_JSON)
				.content("{\"extraDiscount\":0.5}")).andExpect(status().isNotFound());
	}

	@Test
	void listsAndFiltersDiscounted() throws Exception {
		mvc.perform(get("/api/admin/cards").param("setId", "TS-08").param("discounted", "true")
				.header("X-Admin-Token", "secret")).andExpect(status().isOk())
				.andExpect(jsonPath("$.total").value(0));
		mvc.perform(patch("/api/admin/cards/" + id + "/extra-discount")
				.header("X-Admin-Token", "secret").contentType(MediaType.APPLICATION_JSON)
				.content("{\"extraDiscount\":0.5}")).andExpect(status().isOk());
		mvc.perform(get("/api/admin/cards").param("setId", "TS-08").param("discounted", "true")
				.header("X-Admin-Token", "secret")).andExpect(jsonPath("$.total").value(1))
				.andExpect(jsonPath("$.items[0].extraDiscount").value(0.5));
	}
}
