package com.argo.card.admin;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.argo.card.Card;
import com.argo.card.CardRepository;
import com.argo.card.CardSet;
import com.argo.card.CardSetRepository;
import com.argo.card.OptcgCard;
import com.argo.staff.auth.LoginPortal;
import com.argo.staff.auth.StaffAuthService;
import com.argo.staff.auth.StaffRole;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
@Transactional
class AdminCardApiTests {

	@Autowired
	WebApplicationContext wac;
	@Autowired
	CardRepository cards;
	@Autowired
	CardSetRepository sets;
	@Autowired
	StaffAuthService auth;
	@Autowired
	org.springframework.jdbc.core.JdbcTemplate jdbc;

	MockMvc mvc;
	Long id;
	String general;
	String service;

	@BeforeEach
	void setUp() {
		mvc = MockMvcBuilders.webAppContextSetup(wac).build();
		sets.save(new CardSet("TS-08", "Test Set", "booster"));
		Card c = new Card("TS-08|A|");
		c.fill(new OptcgCard("TS08-A", "TS08-A", "TS-08", "Test Set", "Card A", null, "C", "Red",
				"Character", "1", "1000", null, null, null, null, null, 10.0, 1.0, null),
				new BigDecimal("0.9"));
		id = cards.saveAndFlush(c).getId();
		auth.create("gen1", "password-1234", StaffRole.GENERAL);
		auth.create("svc1", "password-1234", StaffRole.SERVICE);
		general = auth.login("gen1", "password-1234", LoginPortal.ADMIN).token();
		service = auth.login("svc1", "password-1234", LoginPortal.ADMIN).token();
	}

	@Test
	void rejectsMissingToken() throws Exception {
		mvc.perform(get("/api/admin/cards")).andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.msg").value("ADMIN_UNAUTHORIZED"));
	}

	@Test
	void rejectsWrongToken() throws Exception {
		mvc.perform(patch("/api/admin/cards/" + id + "/extra-discount")
				.header("Authorization", "Bearer nope").contentType(MediaType.APPLICATION_JSON)
				.content("{\"extraDiscount\":0.4}")).andExpect(status().isUnauthorized());
	}

	@Test
	void serviceRoleIsForbidden() throws Exception {
		mvc.perform(get("/api/admin/cards").header("Authorization", "Bearer " + service))
				.andExpect(status().isForbidden());
		mvc.perform(patch("/api/admin/cards/" + id + "/extra-discount")
				.header("Authorization", "Bearer " + service).contentType(MediaType.APPLICATION_JSON)
				.content("{\"extraDiscount\":0.4}")).andExpect(status().isForbidden())
				.andExpect(jsonPath("$.msg").value("ADMIN_FORBIDDEN"));
	}

	@Test
	void serviceRoleCanStillCheckIdentity() throws Exception {
		mvc.perform(get("/api/admin/auth/me").header("Authorization", "Bearer " + service))
				.andExpect(status().isOk()).andExpect(jsonPath("$.data.role").value("SERVICE"));
	}

	@Test
	void setsExtraDiscount() throws Exception {
		mvc.perform(patch("/api/admin/cards/" + id + "/extra-discount")
				.header("Authorization", "Bearer " + general).contentType(MediaType.APPLICATION_JSON)
				.content("{\"extraDiscount\":0.4}")).andExpect(status().isOk())
				.andExpect(jsonPath("$.data.listPrice").value(9.0))
				.andExpect(jsonPath("$.data.extraDiscount").value(0.4))
				.andExpect(jsonPath("$.data.salePrice").value(3.6));
	}

	@Test
	void rejectsOutOfRange() throws Exception {
		for (String v : new String[] { "0", "1.5", "-1", "0.00001", "null" }) {
			mvc.perform(patch("/api/admin/cards/" + id + "/extra-discount")
					.header("Authorization", "Bearer " + general).contentType(MediaType.APPLICATION_JSON)
					.content("{\"extraDiscount\":" + v + "}")).andExpect(status().isBadRequest());
		}
	}

	@Test
	void showsChineseNameAndKeepsEnglish() throws Exception {
		jdbc.update("insert into card_translation (card_set_id, locale, card_name, updated_at) "
				+ "values ('TS08-A', 'zh-TW', '測試卡', now())");
		mvc.perform(get("/api/admin/cards").param("setId", "TS-08")
				.header("Authorization", "Bearer " + general)).andExpect(status().isOk())
				.andExpect(jsonPath("$.data.items[0].cardName").value("測試卡"))
				.andExpect(jsonPath("$.data.items[0].cardNameEn").value("Card A"));
		mvc.perform(get("/api/admin/cards").param("keyword", "測試")
				.header("Authorization", "Bearer " + general)).andExpect(jsonPath("$.data.total").value(1));
		mvc.perform(patch("/api/admin/cards/" + id + "/extra-discount")
				.header("Authorization", "Bearer " + general).contentType(MediaType.APPLICATION_JSON)
				.content("{\"extraDiscount\":0.5}")).andExpect(jsonPath("$.data.cardName").value("測試卡"));
	}

	@Test
	void fallsBackToEnglishWithoutTranslation() throws Exception {
		mvc.perform(get("/api/admin/cards").param("setId", "TS-08")
				.header("Authorization", "Bearer " + general))
				.andExpect(jsonPath("$.data.items[0].cardName").value("Card A"));
	}

	@Test
	void unknownCardIs404() throws Exception {
		mvc.perform(patch("/api/admin/cards/999999999/extra-discount")
				.header("Authorization", "Bearer " + general).contentType(MediaType.APPLICATION_JSON)
				.content("{\"extraDiscount\":0.5}")).andExpect(status().isNotFound());
	}

	@Test
	void listsAndFiltersDiscounted() throws Exception {
		mvc.perform(get("/api/admin/cards").param("setId", "TS-08").param("discounted", "true")
				.header("Authorization", "Bearer " + general)).andExpect(status().isOk())
				.andExpect(jsonPath("$.data.total").value(0));
		mvc.perform(patch("/api/admin/cards/" + id + "/extra-discount")
				.header("Authorization", "Bearer " + general).contentType(MediaType.APPLICATION_JSON)
				.content("{\"extraDiscount\":0.5}")).andExpect(status().isOk());
		mvc.perform(get("/api/admin/cards").param("setId", "TS-08").param("discounted", "true")
				.header("Authorization", "Bearer " + general)).andExpect(jsonPath("$.data.total").value(1))
				.andExpect(jsonPath("$.data.items[0].extraDiscount").value(0.5));
	}
}
