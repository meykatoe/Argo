package com.argo.admin;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
@Transactional
class AdminCardSetApiTests {

	static final String PW = "password-1234";

	@Autowired
	WebApplicationContext wac;
	@Autowired
	CardRepository cards;
	@Autowired
	CardSetRepository sets;
	@Autowired
	StaffAuthService auth;
	@Autowired
	JdbcTemplate jdbc;

	MockMvc mvc;
	String general;
	String service;
	Card a;
	Card b;
	Card c;

	private Card card(String key, double market) {
		Card x = new Card("TS-70|" + key + "|");
		x.fill(new OptcgCard("TS70-" + key, "TS70-" + key, "TS-70", "Test Set", "Card " + key, null,
				"C", "Red", "Character", "1", "1000", null, null, null, null, null, market, 1.0, null),
				new BigDecimal("0.9"));
		return cards.saveAndFlush(x);
	}

	@BeforeEach
	void setUp() {
		mvc = MockMvcBuilders.webAppContextSetup(wac).build();
		sets.save(new CardSet("TS-70", "Test Set", "booster"));
		a = card("A", 10.0);
		b = card("B", 20.0);
		c = card("C", 30.0);
		c.overridePrice(new BigDecimal("12.34"));
		cards.saveAndFlush(c);
		// 權限來自資料庫，測試自行建立選單與授權
		jdbc.update("insert into admin_menu (parent_id, portal, code, title, path, sort_order) "
				+ "values ((select id from admin_menu where code = 'card'), 'ADMIN', 'card.series', '卡牌系列', '/series', 5)");
		jdbc.update("insert into role_menu (role, menu_id) select 'GENERAL', id from admin_menu where code = 'card.series'");
		auth.create("gen1", PW, StaffRole.GENERAL);
		auth.create("svc1", PW, StaffRole.SERVICE);
		general = "Bearer " + auth.login("gen1", PW, LoginPortal.ADMIN).token();
		service = "Bearer " + auth.login("svc1", PW, LoginPortal.ADMIN).token();
	}

	private ResultActions send(String path, String body) throws Exception {
		return mvc.perform(patch("/api/admin/card-sets/TS-70" + path).header("Authorization", general)
				.contentType(MediaType.APPLICATION_JSON).content(body));
	}

	private int audits(String action) {
		return jdbc.queryForObject("select count(*) from staff_audit_log where action = ? and target_id = 'TS-70'",
				Integer.class, action);
	}

	@Test
	void listShowsStats() throws Exception {
		mvc.perform(get("/api/admin/card-sets").header("Authorization", general)).andExpect(status().isOk())
				.andExpect(jsonPath("$.data[?(@.setId=='TS-70')].cardCount").value(3))
				.andExpect(jsonPath("$.data[?(@.setId=='TS-70')].onSale").value(1))
				.andExpect(jsonPath("$.data[?(@.setId=='TS-70')].minDiscount").value(1.0))
				.andExpect(jsonPath("$.data[?(@.setId=='TS-70')].setNameEn").value("Test Set"));
	}

	@Test
	void permissionComesFromTheDatabase() throws Exception {
		mvc.perform(get("/api/admin/card-sets").header("Authorization", service)).andExpect(status().isForbidden());
		mvc.perform(patch("/api/admin/card-sets/TS-70/on-sale").header("Authorization", service)
				.contentType(MediaType.APPLICATION_JSON).content("{\"onSale\":0}"))
				.andExpect(status().isForbidden());
		mvc.perform(get("/api/admin/card-sets")).andExpect(status().isUnauthorized());
	}

	@Test
	void takesSeriesOffShelfAndAudits() throws Exception {
		send("/on-sale", "{\"onSale\":0}").andExpect(status().isOk()).andExpect(jsonPath("$.data.onSale").value(0));
		assertEquals(false, sets.findById("TS-70").orElseThrow().isOnSale());
		assertEquals(1, audits("CARD_SET_ON_SALE_UPDATE"));
		// 重複送出同樣的值不再記錄
		send("/on-sale", "{\"onSale\":0}").andExpect(status().isOk());
		assertEquals(1, audits("CARD_SET_ON_SALE_UPDATE"));
		send("/on-sale", "{\"onSale\":1}").andExpect(jsonPath("$.data.onSale").value(1));
		assertEquals(2, audits("CARD_SET_ON_SALE_UPDATE"));
	}

	@Test
	void rejectsBadOnSaleBody() throws Exception {
		send("/on-sale", "{}").andExpect(status().isBadRequest());
		send("/on-sale", "{\"onSale\":null}").andExpect(status().isBadRequest());
		// 只接受 0 與 1，不接受布林與其他數字
		send("/on-sale", "{\"onSale\":true}").andExpect(status().isBadRequest());
		send("/on-sale", "{\"onSale\":2}").andExpect(status().isBadRequest());
		send("/on-sale", "{\"onSale\":-1}").andExpect(status().isBadRequest());
	}

	@Test
	void appliesDiscountToWholeSeries() throws Exception {
		send("/extra-discount", "{\"extraDiscount\":0.5}").andExpect(status().isOk())
				.andExpect(jsonPath("$.data.minDiscount").value(0.5)).andExpect(jsonPath("$.data.maxDiscount").value(0.5));
		assertEquals(0, new BigDecimal("4.50").compareTo(cards.findById(a.getId()).orElseThrow().getSalePrice()));
		assertEquals(0, new BigDecimal("9.00").compareTo(cards.findById(b.getId()).orElseThrow().getSalePrice()));
		// 手動定價的卡折扣有記下，但價格不動
		Card over = cards.findById(c.getId()).orElseThrow();
		assertEquals(0, new BigDecimal("12.34").compareTo(over.getSalePrice()));
		assertEquals(0, new BigDecimal("0.5").compareTo(over.getExtraDiscount()));
	}

	@Test
	void auditKeepsPreviousDiscounts() throws Exception {
		a.applyExtraDiscount(new BigDecimal("0.3"), new BigDecimal("0.9"));
		cards.saveAndFlush(a);
		send("/extra-discount", "{\"extraDiscount\":0.5}").andExpect(status().isOk());
		var row = jdbc.queryForMap("select id, username from staff_audit_log where action = 'CARD_SET_EXTRA_DISCOUNT_UPDATE'");
		assertEquals("gen1", row.get("username"));
		Object id = row.get("id");
		assertEquals("3", jdbc.queryForObject("select detail->>'cardCount' from staff_audit_log where id = ?", String.class, id));
		assertEquals("3", jdbc.queryForObject("select detail->>'changedCount' from staff_audit_log where id = ?", String.class, id));
		assertEquals("1", jdbc.queryForObject("select detail->>'overriddenCount' from staff_audit_log where id = ?", String.class, id));
		assertEquals("0.3", jdbc.queryForObject("select detail->'previousDiscounts'->>? from staff_audit_log where id = ?", String.class, String.valueOf(a.getId()), id));
		assertEquals("0.5", jdbc.queryForObject("select detail->>'extraDiscount' from staff_audit_log where id = ?", String.class, id));
	}

	@Test
	void rangeShowsMixedDiscounts() throws Exception {
		a.applyExtraDiscount(new BigDecimal("0.3"), new BigDecimal("0.9"));
		cards.saveAndFlush(a);
		mvc.perform(get("/api/admin/card-sets").header("Authorization", general))
				.andExpect(jsonPath("$.data[?(@.setId=='TS-70')].minDiscount").value(0.3))
				.andExpect(jsonPath("$.data[?(@.setId=='TS-70')].maxDiscount").value(1.0));
	}

	@Test
	void resettingToOneRestoresPrices() throws Exception {
		send("/extra-discount", "{\"extraDiscount\":0.5}").andExpect(status().isOk());
		send("/extra-discount", "{\"extraDiscount\":1}").andExpect(status().isOk());
		assertEquals(0, new BigDecimal("9.00").compareTo(cards.findById(a.getId()).orElseThrow().getSalePrice()));
	}

	@Test
	void rejectsBadDiscountAndUnknownSeries() throws Exception {
		for (String v : new String[] { "0", "1.5", "-1", "null" }) {
			send("/extra-discount", "{\"extraDiscount\":" + v + "}").andExpect(status().isBadRequest());
		}
		mvc.perform(patch("/api/admin/card-sets/NOPE/on-sale").header("Authorization", general)
				.contentType(MediaType.APPLICATION_JSON).content("{\"onSale\":0}")).andExpect(status().isNotFound());
		mvc.perform(patch("/api/admin/card-sets/NOPE/extra-discount").header("Authorization", general)
				.contentType(MediaType.APPLICATION_JSON).content("{\"extraDiscount\":0.5}")).andExpect(status().isNotFound());
	}
}
