package com.argo.customer;

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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
@Transactional
@TestPropertySource(properties = "argo.customer.max-failures=3")
class CustomerAuthTests {

	static final String PW = "correct-horse-1";

	@Autowired
	CustomerAuthService auth;
	@Autowired
	CustomerAccountRepository accounts;
	@Autowired
	JdbcTemplate jdbc;
	@Autowired
	WebApplicationContext wac;

	MockMvc mvc;

	@BeforeEach
	void setUp() {
		mvc = MockMvcBuilders.webAppContextSetup(wac).build();
	}

	private String code(Runnable r) {
		return assertThrows(ApiException.class, r::run).getCode();
	}

	private ResultActions json(String path, String body) throws Exception {
		return mvc.perform(post(path).contentType(MediaType.APPLICATION_JSON).content(body));
	}

	@Test
	void registerStoresHashedPasswordAndLogsIn() {
		var view = auth.register("  Buyer@Test.Local ", PW, " 小明 ");
		assertEquals("buyer@test.local", view.email());
		assertEquals("小明", view.name());
		CustomerAccount c = accounts.findByEmail("buyer@test.local").orElseThrow();
		assertNotEquals(PW, c.getPasswordHash());
		assertTrue(c.getPasswordHash().startsWith("$2"));
		assertTrue(auth.authenticate(view.token()).isPresent());
	}

	@Test
	void tokenIsStoredHashedOnly() {
		var view = auth.register("a@test.local", PW, null);
		accounts.flush();
		assertEquals(0, jdbc.queryForObject("select count(*) from customer_session where token_hash = ?", Integer.class, view.token()));
		assertEquals(1, jdbc.queryForObject("select count(*) from customer_session where customer_id = (select id from customer_account where email = 'a@test.local')", Integer.class));
	}

	@Test
	void emailIsCaseInsensitiveAndUnique() {
		auth.register("Dup@Test.Local", PW, null);
		assertEquals("EMAIL_TAKEN", code(() -> auth.register("dup@test.local", PW, null)));
		assertEquals("dup@test.local", auth.login("DUP@test.local", PW).email());
	}

	@Test
	void rejectsTooLongPasswordInBytes() {
		// 25 個中文字是 75 位元組，超過 bcrypt 上限
		String longPw = "密".repeat(25);
		ApiException e = assertThrows(ApiException.class, () -> auth.register("b@test.local", longPw, null));
		assertEquals("VALIDATION_ERROR", e.getCode());
		assertTrue(e.getDetails().containsKey("password"));
	}

	@Test
	void wrongPasswordAndUnknownEmailLookAlike() {
		auth.register("c@test.local", PW, null);
		assertEquals("LOGIN_FAILED", code(() -> auth.login("c@test.local", "wrong-password")));
		assertEquals("LOGIN_FAILED", code(() -> auth.login("nobody@test.local", PW)));
	}

	@Test
	void locksAfterRepeatedFailures() {
		auth.register("d@test.local", PW, null);
		for (int i = 0; i < 3; i++) {
			assertEquals("LOGIN_FAILED", code(() -> auth.login("d@test.local", "wrong-password")));
		}
		assertEquals("LOGIN_LOCKED", code(() -> auth.login("d@test.local", PW)));
	}

	@Test
	void disabledAccountCannotLoginAndSessionDies() {
		var view = auth.register("e@test.local", PW, null);
		accounts.findByEmail("e@test.local").orElseThrow().setDisabled(true);
		accounts.flush();
		assertFalse(auth.authenticate(view.token()).isPresent());
		assertEquals("LOGIN_FAILED", code(() -> auth.login("e@test.local", PW)));
		assertEquals(1, jdbc.queryForObject("select disabled from customer_account where email = 'e@test.local'", Integer.class));
	}

	@Test
	void newAccountsAreNormal() {
		auth.register("f@test.local", PW, null);
		assertEquals(0, jdbc.queryForObject("select disabled from customer_account where email = 'f@test.local'", Integer.class));
		assertThrows(org.springframework.dao.DataAccessException.class,
				() -> jdbc.update("update customer_account set disabled = 2"));
	}

	@Test
	void logoutInvalidatesToken() {
		var view = auth.register("g@test.local", PW, null);
		auth.logout(view.token());
		assertFalse(auth.authenticate(view.token()).isPresent());
	}

	@Test
	void staffTokensDoNotWorkForCustomersAndViceVersa() {
		var view = auth.register("h@test.local", PW, null);
		assertFalse(auth.authenticate("not-a-real-token").isPresent());
		assertFalse(auth.authenticate(null).isPresent());
		assertTrue(auth.authenticate(view.token()).isPresent());
	}

	@Test
	void endpointsRegisterLoginMeLogout() throws Exception {
		String reg = json("/api/auth/register", "{\"email\":\"i@test.local\",\"password\":\"" + PW + "\",\"name\":\"阿明\"}")
				.andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200))
				.andExpect(jsonPath("$.data.email").value("i@test.local"))
				.andReturn().getResponse().getContentAsString();
		String token = reg.replaceAll(".*\"token\":\"([^\"]+)\".*", "$1");
		mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
				.andExpect(status().isOk()).andExpect(jsonPath("$.data.name").value("阿明"));
		mvc.perform(post("/api/auth/logout").header("Authorization", "Bearer " + token)).andExpect(status().isOk());
		mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token)).andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.msg").value("UNAUTHORIZED"));
		json("/api/auth/login", "{\"email\":\"I@test.local\",\"password\":\"" + PW + "\"}")
				.andExpect(status().isOk()).andExpect(jsonPath("$.data.email").value("i@test.local"));
	}

	@Test
	void endpointValidation() throws Exception {
		json("/api/auth/register", "{\"email\":\"not-an-email\",\"password\":\"" + PW + "\"}")
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.msg").value("VALIDATION_ERROR"))
				.andExpect(jsonPath("$.data.email").exists());
		json("/api/auth/register", "{\"email\":\"j@test.local\",\"password\":\"short\"}")
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.data.password").exists());
		json("/api/auth/register", "{\"email\":\"k@test.local\",\"password\":\"" + PW + "\"}").andExpect(status().isOk());
		json("/api/auth/register", "{\"email\":\"k@test.local\",\"password\":\"" + PW + "\"}")
				.andExpect(status().isConflict()).andExpect(jsonPath("$.msg").value("EMAIL_TAKEN"));
		json("/api/auth/login", "{\"email\":\"k@test.local\",\"password\":\"bad-password\"}")
				.andExpect(status().isUnauthorized()).andExpect(jsonPath("$.msg").value("LOGIN_FAILED"));
	}

	@Test
	void meNeedsToken() throws Exception {
		mvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
		mvc.perform(get("/api/me/orders")).andExpect(status().isUnauthorized());
	}
}
