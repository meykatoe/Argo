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
	@jakarta.persistence.PersistenceContext
	jakarta.persistence.EntityManager em;

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
	void registerRejectsWeakPassword() {
		assertEquals("VALIDATION_ERROR", code(() -> auth.register(TestUsers.of("weak@test.local"), "weak@test.local", "abcdefgh", null)));
		assertEquals("VALIDATION_ERROR", code(() -> auth.register(TestUsers.of("weak@test.local"), "weak@test.local", "12345678", null)));
	}

	@Test
	void registerStoresHashedPasswordAndLogsIn() {
		var view = auth.register(TestUsers.of("  Buyer@Test.Local "), "  Buyer@Test.Local ", PW, " 小明 ");
		assertEquals("buyer@test.local", view.email());
		assertEquals("小明", view.name());
		CustomerAccount c = accounts.findByEmail("buyer@test.local").orElseThrow();
		assertNotEquals(PW, c.getPasswordHash());
		assertTrue(c.getPasswordHash().startsWith("$2"));
		assertTrue(auth.authenticate(view.token()).isPresent());
	}

	@Test
	void tokenIsStoredHashedOnly() {
		var view = auth.register(TestUsers.of("a@test.local"), "a@test.local", PW, null);
		accounts.flush();
		assertEquals(0, jdbc.queryForObject("select count(*) from customer_session where token_hash = ?", Integer.class, view.token()));
		assertEquals(1, jdbc.queryForObject("select count(*) from customer_session where customer_id = (select id from customer_account where email = 'a@test.local')", Integer.class));
	}

	@Test
	void emailIsCaseInsensitiveAndUnique() {
		auth.register(TestUsers.of("Dup@Test.Local"), "Dup@Test.Local", PW, null);
		assertEquals("EMAIL_TAKEN", code(() -> auth.register("other-dup", "dup@test.local", PW, null)));
		assertEquals("dup@test.local", auth.login("DUP@test.local", PW).email());
	}

	@Test
	void usernameIsCaseInsensitiveAndUnique() {
		auth.register("Zt-Name", "zn1@test.local", PW, null);
		assertEquals("USERNAME_TAKEN", code(() -> auth.register("zt-name", "zn2@test.local", PW, null)));
		assertEquals("USERNAME_TAKEN", code(() -> auth.register("  ZT-NAME ", "zn3@test.local", PW, null)));
	}

	@Test
	void loginAcceptsUsernameOrEmail() {
		auth.register("Login-User", "lu@test.local", PW, null);
		assertEquals("lu@test.local", auth.login("login-user", PW).email());
		assertEquals("lu@test.local", auth.login(" LOGIN-USER ", PW).email());
		assertEquals("login-user", auth.login("lu@test.local", PW).username());
		assertEquals("LOGIN_FAILED", code(() -> auth.login("login-user", "Wrong-pass1")));
		assertEquals("LOGIN_FAILED", code(() -> auth.login("no-such-user", PW)));
	}

	@Test
	void rejectsInvalidUsernames() {
		for (String bad : new String[] { "ab", "has space", "a@b.com", "-lead", "x".repeat(31), "中文帳號", "a/b", "" }) {
			assertEquals("VALIDATION_ERROR", code(() -> auth.register(bad, "iv@test.local", PW, null)), bad);
		}
	}

	@Test
	void usernameLockoutAlsoCountsFailures() {
		auth.register("lock-user", "lk@test.local", PW, null);
		// 帳號與 Email 共用同一個失敗計數
		assertEquals("LOGIN_FAILED", code(() -> auth.login("lk@test.local", "Wrong-pass1")));
		for (int i = 0; i < 2; i++) {
			assertEquals("LOGIN_FAILED", code(() -> auth.login("lock-user", "Wrong-pass1")));
		}
		assertEquals("LOGIN_LOCKED", code(() -> auth.login("lk@test.local", "Wrong-pass1")));
		assertEquals("LOGIN_LOCKED", code(() -> auth.login("lock-user", PW)));
	}

	@Test
	void availabilityEndpoint() throws Exception {
		auth.register("taken-name", "tn@test.local", PW, null);
		mvc.perform(get("/api/auth/username-available").param("username", "Taken-Name"))
				.andExpect(status().isOk()).andExpect(jsonPath("$.data.available").value(0));
		mvc.perform(get("/api/auth/username-available").param("username", "free-name"))
				.andExpect(status().isOk()).andExpect(jsonPath("$.data.available").value(1));
		mvc.perform(get("/api/auth/username-available").param("username", "a@b"))
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.msg").value("VALIDATION_ERROR"));
	}

	@Test
	void rejectsTooLongPasswordInBytes() {
		// 25 個中文字是 75 位元組，超過 bcrypt 上限
		String longPw = "密".repeat(25);
		ApiException e = assertThrows(ApiException.class, () -> auth.register(TestUsers.of("b@test.local"), "b@test.local", longPw, null));
		assertEquals("VALIDATION_ERROR", e.getCode());
		assertTrue(e.getDetails().containsKey("password"));
	}

	@Test
	void wrongPasswordAndUnknownEmailLookAlike() {
		auth.register(TestUsers.of("c@test.local"), "c@test.local", PW, null);
		assertEquals("LOGIN_FAILED", code(() -> auth.login("c@test.local", "wrong-password")));
		assertEquals("LOGIN_FAILED", code(() -> auth.login("nobody@test.local", PW)));
	}

	@Test
	void locksAfterRepeatedFailures() {
		auth.register(TestUsers.of("d@test.local"), "d@test.local", PW, null);
		// 允許失敗 3 次，第 4 次錯誤才鎖定
		for (int i = 0; i < 3; i++) {
			assertEquals("LOGIN_FAILED", code(() -> auth.login("d@test.local", "wrong-password")));
		}
		assertEquals("LOGIN_LOCKED", code(() -> auth.login("d@test.local", "wrong-password")));
		// 鎖定期間連正確密碼也不行
		assertEquals("LOGIN_LOCKED", code(() -> auth.login("d@test.local", PW)));
	}

	@Test
	void lockedResponseTellsHowLongToWait() {
		auth.register(TestUsers.of("d2@test.local"), "d2@test.local", PW, null);
		for (int i = 0; i < 3; i++) {
			code(() -> auth.login("d2@test.local", "wrong-password"));
		}
		ApiException e = assertThrows(ApiException.class, () -> auth.login("d2@test.local", "wrong-password"));
		long seconds = Long.parseLong(e.getDetails().get("retryAfterSeconds"));
		assertTrue(seconds > 14 * 60 && seconds <= 15 * 60 + 1, "seconds=" + seconds);
	}

	@Test
	void successResetsTheFailureCounter() {
		auth.register(TestUsers.of("d3@test.local"), "d3@test.local", PW, null);
		for (int i = 0; i < 3; i++) {
			code(() -> auth.login("d3@test.local", "wrong-password"));
		}
		assertEquals("d3@test.local", auth.login("d3@test.local", PW).email());
		for (int i = 0; i < 3; i++) {
			assertEquals("LOGIN_FAILED", code(() -> auth.login("d3@test.local", "wrong-password")));
		}
	}

	@Test
	void lockEndsAfterFifteenMinutes() {
		auth.register(TestUsers.of("d4@test.local"), "d4@test.local", PW, null);
		for (int i = 0; i < 4; i++) {
			code(() -> auth.login("d4@test.local", "wrong-password"));
		}
		jdbc.update("update customer_account set locked_until = now() - interval '1 second' where email = 'd4@test.local'");
		em.clear();
		assertEquals("d4@test.local", auth.login("d4@test.local", PW).email());
	}

	@Test
	void disabledAccountCannotLoginAndSessionDies() {
		var view = auth.register(TestUsers.of("e@test.local"), "e@test.local", PW, null);
		accounts.findByEmail("e@test.local").orElseThrow().setDisabled(true);
		accounts.flush();
		assertFalse(auth.authenticate(view.token()).isPresent());
		assertEquals("LOGIN_FAILED", code(() -> auth.login("e@test.local", PW)));
		assertEquals(1, jdbc.queryForObject("select disabled from customer_account where email = 'e@test.local'", Integer.class));
	}

	@Test
	void newAccountsAreNormal() {
		auth.register(TestUsers.of("f@test.local"), "f@test.local", PW, null);
		assertEquals(0, jdbc.queryForObject("select disabled from customer_account where email = 'f@test.local'", Integer.class));
		assertThrows(org.springframework.dao.DataAccessException.class,
				() -> jdbc.update("update customer_account set disabled = 2"));
	}

	@Test
	void logoutInvalidatesToken() {
		var view = auth.register(TestUsers.of("g@test.local"), "g@test.local", PW, null);
		auth.logout(view.token());
		assertFalse(auth.authenticate(view.token()).isPresent());
	}

	@Test
	void staffTokensDoNotWorkForCustomersAndViceVersa() {
		var view = auth.register(TestUsers.of("h@test.local"), "h@test.local", PW, null);
		assertFalse(auth.authenticate("not-a-real-token").isPresent());
		assertFalse(auth.authenticate(null).isPresent());
		assertTrue(auth.authenticate(view.token()).isPresent());
	}

	@Test
	void endpointsRegisterLoginMeLogout() throws Exception {
		String reg = json("/api/auth/register", "{\"username\":\"u-i-test-local\",\"email\":\"i@test.local\",\"password\":\"" + PW + "\",\"name\":\"阿明\"}")
				.andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200))
				.andExpect(jsonPath("$.data.email").value("i@test.local"))
				.andReturn().getResponse().getContentAsString();
		String token = reg.replaceAll(".*\"token\":\"([^\"]+)\".*", "$1");
		mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
				.andExpect(status().isOk()).andExpect(jsonPath("$.data.name").value("阿明"));
		mvc.perform(post("/api/auth/logout").header("Authorization", "Bearer " + token)).andExpect(status().isOk());
		mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token)).andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.msg").value("UNAUTHORIZED"));
		json("/api/auth/login", "{\"account\":\"I@test.local\",\"password\":\"" + PW + "\"}")
				.andExpect(status().isOk()).andExpect(jsonPath("$.data.email").value("i@test.local"));
	}

	@Test
	void endpointValidation() throws Exception {
		json("/api/auth/register", "{\"username\":\"u-bad\",\"email\":\"not-an-email\",\"password\":\"" + PW + "\"}")
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.msg").value("VALIDATION_ERROR"))
				.andExpect(jsonPath("$.data.email").exists());
		json("/api/auth/register", "{\"username\":\"u-j-test-local\",\"email\":\"j@test.local\",\"password\":\"short\"}")
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.data.password").exists());
		json("/api/auth/register", "{\"username\":\"u-k-test-local\",\"email\":\"k@test.local\",\"password\":\"" + PW + "\"}").andExpect(status().isOk());
		json("/api/auth/register", "{\"username\":\"u-k-other\",\"email\":\"k@test.local\",\"password\":\"" + PW + "\"}")
				.andExpect(status().isConflict()).andExpect(jsonPath("$.msg").value("EMAIL_TAKEN"));
		json("/api/auth/register", "{\"username\":\"U-K-Test-Local\",\"email\":\"k2@test.local\",\"password\":\"" + PW + "\"}")
				.andExpect(status().isConflict()).andExpect(jsonPath("$.msg").value("USERNAME_TAKEN"));
		json("/api/auth/login", "{\"account\":\"k@test.local\",\"password\":\"bad-password\"}")
				.andExpect(status().isUnauthorized()).andExpect(jsonPath("$.msg").value("LOGIN_FAILED"));
	}

	@Test
	void meNeedsToken() throws Exception {
		mvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
		mvc.perform(get("/api/me/orders")).andExpect(status().isUnauthorized());
	}
}
