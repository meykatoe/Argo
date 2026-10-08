package com.argo.staff.audit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.argo.card.Card;
import com.argo.card.CardRepository;
import com.argo.card.CardSet;
import com.argo.card.CardSetRepository;
import com.argo.card.OptcgCard;
import com.argo.common.ApiException;
import com.argo.staff.auth.LoginPortal;
import com.argo.staff.auth.StaffAuthService;
import com.argo.staff.auth.StaffRole;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
@Transactional
class AuditLogTests {

	@Autowired
	StaffAuthService auth;
	@Autowired
	CardRepository cards;
	@Autowired
	CardSetRepository sets;
	@Autowired
	JdbcTemplate jdbc;
	@Autowired
	WebApplicationContext wac;

	MockMvc mvc;
	Long cardId;

	@BeforeEach
	void setUp() {
		mvc = MockMvcBuilders.webAppContextSetup(wac).build();
		sets.save(new CardSet("TS-09", "Test Set", "booster"));
		Card c = new Card("TS-09|A|");
		c.fill(new OptcgCard("TS09-A", "TS09-A", "TS-09", "Test Set", "Card A", null, "C", "Red",
				"Character", "1", "1000", null, null, null, null, null, 10.0, 1.0, null),
				new BigDecimal("0.9"));
		cardId = cards.saveAndFlush(c).getId();
		auth.create("gen1", "Password-1234", StaffRole.GENERAL);
		auth.create("svc1", "Password-1234", StaffRole.SERVICE);
	}

	private List<Map<String, Object>> logs(String where, Object... args) {
		return jdbc.queryForList("select * from staff_audit_log where " + where + " order by id", args);
	}

	@Test
	void loginSuccessIsRecordedWithRoleAndIp() throws Exception {
		mvc.perform(post("/api/admin/auth/login").contentType(MediaType.APPLICATION_JSON)
				.header("User-Agent", "JUnit")
				.content("{\"username\":\"gen1\",\"password\":\"Password-1234\"}"));
		var rows = logs("action = 'LOGIN_SUCCESS' and username = 'gen1'");
		assertEquals(1, rows.size());
		assertEquals("GENERAL", rows.get(0).get("role"));
		assertEquals("127.0.0.1", rows.get(0).get("ip"));
		assertEquals("JUnit", rows.get(0).get("user_agent"));
		assertEquals(1, ((Number) rows.get(0).get("success")).intValue());
	}

	@Test
	void failedLoginsAreRecordedWithoutPasswords() {
		assertThrows(ApiException.class, () -> auth.login("gen1", "wrong-password-xyz", LoginPortal.ADMIN));
		assertThrows(ApiException.class, () -> auth.login("ghost", "wrong-password-xyz", LoginPortal.ADMIN));
		var bad = logs("action = 'LOGIN_FAILED' and username = 'gen1'");
		assertEquals(1, bad.size());
		assertEquals(0, ((Number) bad.get(0).get("success")).intValue());
		assertEquals("BAD_PASSWORD", jdbc.queryForObject(
				"select detail->>'reason' from staff_audit_log where id = ?", String.class, bad.get(0).get("id")));
		var ghost = logs("action = 'LOGIN_FAILED' and username = 'ghost'");
		assertEquals(1, ghost.size());
		assertNull(ghost.get(0).get("staff_id"));
		assertEquals(0, logs("detail::text like '%wrong-password-xyz%'").size());
	}

	@Test
	void logoutIsRecorded() {
		String token = auth.login("gen1", "Password-1234", LoginPortal.ADMIN).token();
		auth.logout(token);
		assertEquals(1, logs("action = 'LOGOUT' and username = 'gen1'").size());
	}

	@Test
	void discountChangeRecordsActorAndBeforeAfter() throws Exception {
		String token = auth.login("gen1", "Password-1234", LoginPortal.ADMIN).token();
		mvc.perform(patch("/api/admin/cards/" + cardId + "/extra-discount")
				.header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
				.content("{\"extraDiscount\":0.4}"));
		var rows = logs("action = 'CARD_EXTRA_DISCOUNT_UPDATE'");
		assertEquals(1, rows.size());
		var row = rows.get(0);
		assertEquals("gen1", row.get("username"));
		assertEquals("CARD", row.get("target_type"));
		assertEquals(String.valueOf(cardId), row.get("target_id"));
		var id = row.get("id");
		assertEquals("1", jdbc.queryForObject("select detail->>'extraDiscountBefore' from staff_audit_log where id = ?", String.class, id));
		assertEquals("0.4", jdbc.queryForObject("select detail->>'extraDiscountAfter' from staff_audit_log where id = ?", String.class, id));
		assertEquals("9.00", jdbc.queryForObject("select detail->>'salePriceBefore' from staff_audit_log where id = ?", String.class, id));
		assertEquals("3.60", jdbc.queryForObject("select detail->>'salePriceAfter' from staff_audit_log where id = ?", String.class, id));
	}

	@Test
	void forbiddenAccessIsRecorded() throws Exception {
		String token = auth.login("svc1", "Password-1234", LoginPortal.ADMIN).token();
		mvc.perform(get("/api/admin/cards").header("Authorization", "Bearer " + token));
		var rows = logs("action = 'ACCESS_DENIED' and username = 'svc1'");
		assertEquals(1, rows.size());
		assertEquals("SERVICE", rows.get(0).get("role"));
		assertEquals("/api/admin/cards", rows.get(0).get("target_id"));
	}

	@Test
	void logsCannotBeUpdatedOrDeleted() {
		auth.login("gen1", "Password-1234", LoginPortal.ADMIN);
		assertThrows(DataAccessException.class, () -> jdbc.update("update staff_audit_log set username = 'x'"));
	}
}
