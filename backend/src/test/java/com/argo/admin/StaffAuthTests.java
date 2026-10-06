package com.argo.admin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
@Transactional
@TestPropertySource(properties = "argo.admin.max-failures=3")
class StaffAuthTests {

	@Autowired
	StaffAuthService auth;
	@Autowired
	StaffAccountRepository accounts;
	@Autowired
	StaffSessionRepository sessions;
	@Autowired
	org.springframework.jdbc.core.JdbcTemplate jdbc;
	@Autowired
	WebApplicationContext wac;

	MockMvc mvc;

	@BeforeEach
	void setUp() {
		mvc = MockMvcBuilders.webAppContextSetup(wac).build();
		auth.create("Alice", "password-1234", StaffRole.GENERAL);
	}

	private String code(Runnable r) {
		return assertThrows(ApiException.class, r::run).getCode();
	}

	@Test
	void passwordIsHashed() {
		StaffAccount a = accounts.findByUsername("alice").orElseThrow();
		assertNotEquals("password-1234", a.getPasswordHash());
		assertTrue(a.getPasswordHash().startsWith("$2"));
	}

	@Test
	void createValidatesInput() {
		assertThrows(IllegalArgumentException.class, () -> auth.create("alice", "password-1234", StaffRole.ADMIN));
		assertThrows(IllegalArgumentException.class, () -> auth.create("ALICE", "password-1234", StaffRole.ADMIN));
		assertThrows(IllegalArgumentException.class, () -> auth.create("bob", "short", StaffRole.ADMIN));
		assertThrows(IllegalArgumentException.class, () -> auth.create("a b", "password-1234", StaffRole.ADMIN));
	}

	@Test
	void loginIsCaseInsensitiveOnUsername() {
		var res = auth.login(" ALICE ", "password-1234", LoginPortal.ADMIN);
		assertEquals("alice", res.username());
		assertEquals(StaffRole.GENERAL, res.role());
		assertTrue(auth.authenticate(res.token()).isPresent());
	}

	@Test
	void tokenIsStoredHashedOnly() {
		var res = auth.login("alice", "password-1234", LoginPortal.ADMIN);
		assertFalse(sessions.existsById(res.token()));
		Long staffId = accounts.findByUsername("alice").orElseThrow().getId();
		assertEquals(1, jdbc.queryForObject(
				"select count(*) from staff_session where staff_id = ?", Integer.class, staffId));
	}

	@Test
	void wrongPasswordAndUnknownUserLookAlike() {
		assertEquals("LOGIN_FAILED", code(() -> auth.login("alice", "wrong-password", LoginPortal.ADMIN)));
		assertEquals("LOGIN_FAILED", code(() -> auth.login("nobody", "password-1234", LoginPortal.ADMIN)));
	}

	@Test
	void locksAfterRepeatedFailures() {
		for (int i = 0; i < 3; i++) {
			assertEquals("LOGIN_FAILED", code(() -> auth.login("alice", "wrong-password", LoginPortal.ADMIN)));
		}
		assertEquals("LOGIN_LOCKED", code(() -> auth.login("alice", "password-1234", LoginPortal.ADMIN)));
	}

	@Test
	void disabledAccountCannotLoginOrUseSession() {
		var res = auth.login("alice", "password-1234", LoginPortal.ADMIN);
		accounts.findByUsername("alice").orElseThrow().setEnabled(false);
		assertTrue(auth.authenticate(res.token()).isEmpty());
		assertEquals("LOGIN_FAILED", code(() -> auth.login("alice", "password-1234", LoginPortal.ADMIN)));
	}

	@Test
	void logoutInvalidatesToken() {
		var res = auth.login("alice", "password-1234", LoginPortal.ADMIN);
		auth.logout(res.token());
		assertTrue(auth.authenticate(res.token()).isEmpty());
	}

	@Test
	void loginEndpointIsOpenAndMeNeedsToken() throws Exception {
		mvc.perform(get("/api/admin/auth/me")).andExpect(status().isUnauthorized());
		String body = mvc.perform(post("/api/admin/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content("{\"username\":\"alice\",\"password\":\"password-1234\"}"))
				.andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
		String token = body.replaceAll(".*\"token\":\"([^\"]+)\".*", "$1");
		mvc.perform(get("/api/admin/auth/me").header("Authorization", "Bearer " + token))
				.andExpect(status().isOk()).andExpect(jsonPath("$.role").value("GENERAL"));
	}

	@Test
	void badLoginReturns401() throws Exception {
		mvc.perform(post("/api/admin/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content("{\"username\":\"alice\",\"password\":\"nope-nope-nope\"}"))
				.andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("LOGIN_FAILED"));
	}
}
