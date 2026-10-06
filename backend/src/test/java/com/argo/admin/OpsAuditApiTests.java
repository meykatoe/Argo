package com.argo.admin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.argo.common.ApiException;
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
class OpsAuditApiTests {

	static final String PW = "password-1234";

	@Autowired
	StaffAuthService auth;
	@Autowired
	JdbcTemplate jdbc;
	@Autowired
	WebApplicationContext wac;

	MockMvc mvc;
	String ops;

	@BeforeEach
	void setUp() {
		mvc = MockMvcBuilders.webAppContextSetup(wac).build();
		auth.create("ops1", PW, StaffRole.OPS);
		auth.create("adm1", PW, StaffRole.ADMIN);
		auth.create("gen1", PW, StaffRole.GENERAL);
		auth.create("svc1", PW, StaffRole.SERVICE);
		ops = auth.login("ops1", PW, LoginPortal.OPS).token();
	}

	private String bearer(String user) {
		return "Bearer " + auth.login(user, PW, LoginPortal.ADMIN).token();
	}

	@Test
	void everyOtherRoleIsForbidden() throws Exception {
		for (String u : new String[] { "adm1", "gen1", "svc1" }) {
			mvc.perform(get("/api/ops/audit-logs").header("Authorization", bearer(u)))
					.andExpect(status().isForbidden()).andExpect(jsonPath("$.msg").value("ADMIN_FORBIDDEN"));
		}
	}

	@Test
	void anonymousIsUnauthorized() throws Exception {
		mvc.perform(get("/api/ops/audit-logs")).andExpect(status().isUnauthorized());
	}

	@Test
	void opsSeesLogsNewestFirst() throws Exception {
		mvc.perform(get("/api/ops/audit-logs").header("Authorization", "Bearer " + ops))
				.andExpect(status().isOk()).andExpect(jsonPath("$.data.items[0].username").value("ops1"))
				.andExpect(jsonPath("$.data.items[0].action").value("LOGIN_SUCCESS"));
	}

	@Test
	void filtersByUsernameActionAndSuccess() throws Exception {
		assertThrows(ApiException.class, () -> auth.login("gen1", "bad-password-1", LoginPortal.ADMIN));
		mvc.perform(get("/api/ops/audit-logs").header("Authorization", "Bearer " + ops)
				.param("username", "GEN").param("action", "LOGIN_FAILED").param("success", "0"))
				.andExpect(status().isOk()).andExpect(jsonPath("$.data.total").value(1))
				.andExpect(jsonPath("$.data.items[0].detail.reason").value("BAD_PASSWORD"));
		mvc.perform(get("/api/ops/audit-logs").header("Authorization", "Bearer " + ops)
				.param("username", "%")).andExpect(jsonPath("$.data.total").value(0));
	}

	@Test
	void rejectsBadParams() throws Exception {
		String h = "Bearer " + ops;
		mvc.perform(get("/api/ops/audit-logs").header("Authorization", h).param("action", "NOPE"))
				.andExpect(status().isBadRequest());
		mvc.perform(get("/api/ops/audit-logs").header("Authorization", h).param("size", "101"))
				.andExpect(status().isBadRequest());
		mvc.perform(get("/api/ops/audit-logs").header("Authorization", h)
				.param("from", "2026-02-01T00:00:00Z").param("to", "2026-01-01T00:00:00Z"))
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.msg").value("INVALID_RANGE"));
	}

	@Test
	void viewingIsItselfRecorded() throws Exception {
		mvc.perform(get("/api/ops/audit-logs").header("Authorization", "Bearer " + ops)
				.param("username", "gen1")).andExpect(status().isOk());
		assertEquals(1, jdbc.queryForObject("select count(*) from staff_audit_log "
				+ "where action = 'AUDIT_LOG_VIEWED' and username = 'ops1' and detail->>'username' = 'gen1'",
				Integer.class));
	}

	@Test
	void portalsAreSeparated() throws Exception {
		ApiException a = assertThrows(ApiException.class, () -> auth.login("ops1", PW, LoginPortal.ADMIN));
		assertEquals("LOGIN_FAILED", a.getCode());
		ApiException b = assertThrows(ApiException.class, () -> auth.login("adm1", PW, LoginPortal.OPS));
		assertEquals("LOGIN_FAILED", b.getCode());
		assertEquals(2, jdbc.queryForObject(
				"select count(*) from staff_audit_log where detail->>'reason' = 'WRONG_PORTAL'", Integer.class));
		mvc.perform(post("/api/ops/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content("{\"username\":\"gen1\",\"password\":\"" + PW + "\"}"))
				.andExpect(status().isUnauthorized());
		mvc.perform(post("/api/ops/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content("{\"username\":\"ops1\",\"password\":\"" + PW + "\"}"))
				.andExpect(status().isOk()).andExpect(jsonPath("$.data.role").value("OPS"));
	}

	@Test
	void opsTokenCannotUseBusinessEndpoints() throws Exception {
		mvc.perform(get("/api/admin/cards").header("Authorization", "Bearer " + ops))
				.andExpect(status().isForbidden());
		mvc.perform(get("/api/ops/auth/me").header("Authorization", "Bearer " + ops))
				.andExpect(status().isOk()).andExpect(jsonPath("$.data.role").value("OPS"));
	}
}
