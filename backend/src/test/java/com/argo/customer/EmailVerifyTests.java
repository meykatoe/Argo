package com.argo.customer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.argo.common.mail.Mailer;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

// 先驗證信箱才建立帳號
@SpringBootTest
@Transactional
class EmailVerifyTests {

	static final String PW = "correct-horse-1";

	@Autowired
	CustomerAccountRepository accounts;
	@Autowired
	JdbcTemplate jdbc;
	@Autowired
	WebApplicationContext wac;
	@jakarta.persistence.PersistenceContext
	jakarta.persistence.EntityManager em;
	@MockitoBean
	Mailer mailer;

	MockMvc mvc;
	String id;
	String username;
	String email;

	@BeforeEach
	void setUp() {
		mvc = MockMvcBuilders.webAppContextSetup(wac).build();
		reset(mailer);
		id = UUID.randomUUID().toString().substring(0, 8);
		username = "ev-" + id;
		email = "ev-" + id + "@example.com";
	}

	private ResultActions json(String path, String body) throws Exception {
		return mvc.perform(post(path).contentType(MediaType.APPLICATION_JSON).content(body));
	}

	private ResultActions register() throws Exception {
		return json("/api/auth/register", "{\"username\":\"" + username + "\",\"email\":\"" + email
				+ "\",\"password\":\"" + PW + "\",\"name\":\"阿明\"}");
	}

	private ResultActions verifyWith(String token) throws Exception {
		return json("/api/auth/verify-email", "{\"token\":\"" + token + "\"}");
	}

	private void resend() throws Exception {
		json("/api/auth/resend-verification", "{\"email\":\"" + email + "\"}").andExpect(status().isOk());
	}

	private String mailedToken() {
		ArgumentCaptor<String> body = ArgumentCaptor.forClass(String.class);
		verify(mailer, times(1)).send(eq(email), anyString(), body.capture());
		Matcher m = Pattern.compile("verify-email\\?token=([\\w-]+)").matcher(body.getValue());
		assertTrue(m.find());
		String t = m.group(1);
		reset(mailer);
		return t;
	}

	private void backdatePending() {
		em.flush();
		jdbc.update("update customer_pending_registration set created_at = now() - interval '5 minutes' "
				+ "where email = ?", email);
		em.clear();
	}

	@Test
	void registerCreatesNoAccountUntilVerified() throws Exception {
		register().andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200))
				.andExpect(jsonPath("$.data").doesNotExist());
		assertTrue(accounts.findByEmail(email).isEmpty());
		json("/api/auth/login", "{\"account\":\"" + email + "\",\"password\":\"" + PW + "\"}")
				.andExpect(status().isUnauthorized());
		mailedToken();
	}

	@Test
	void linkCreatesAccountAndLogsIn() throws Exception {
		register();
		String t = mailedToken();
		String body = verifyWith(t).andExpect(status().isOk()).andExpect(jsonPath("$.data.email").value(email))
				.andExpect(jsonPath("$.data.username").value(username)).andExpect(jsonPath("$.data.name").value("阿明"))
				.andReturn().getResponse().getContentAsString();
		String token = body.replaceAll(".*\"token\":\"([^\"]+)\".*", "$1");
		mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token)).andExpect(status().isOk());
		json("/api/auth/login", "{\"account\":\"" + username + "\",\"password\":\"" + PW + "\"}")
				.andExpect(status().isOk());
	}

	@Test
	void linkWorksOnlyOnce() throws Exception {
		register();
		String t = mailedToken();
		verifyWith(t).andExpect(status().isOk());
		verifyWith(t).andExpect(status().isBadRequest()).andExpect(jsonPath("$.msg").value("TOKEN_INVALID"))
				.andExpect(jsonPath("$.code").value(2008));
	}

	@Test
	void unknownAndExpiredLinksAreRejected() throws Exception {
		verifyWith("nope").andExpect(status().isBadRequest());
		register();
		String t = mailedToken();
		em.flush();
		jdbc.update("update customer_pending_registration set expires_at = now() - interval '1 minute' "
				+ "where email = ?", email);
		em.clear();
		verifyWith(t).andExpect(status().isBadRequest());
		assertTrue(accounts.findByEmail(email).isEmpty());
	}

	@Test
	void existingEmailOrUsernameIsRefusedBeforeMailing() throws Exception {
		register();
		verifyWith(mailedToken()).andExpect(status().isOk());
		register().andExpect(status().isConflict()).andExpect(jsonPath("$.msg").value("USERNAME_TAKEN"));
		username = "other-" + id;
		register().andExpect(status().isConflict()).andExpect(jsonPath("$.msg").value("EMAIL_TAKEN"));
		verify(mailer, never()).send(anyString(), anyString(), anyString());
	}

	@Test
	void laterRegistrationReplacesEarlierOne() throws Exception {
		register();
		String first = mailedToken();
		backdatePending();
		register().andExpect(status().isOk());
		String second = mailedToken();
		// 後一次註冊取代前一次，舊連結失效
		verifyWith(first).andExpect(status().isBadRequest());
		verifyWith(second).andExpect(status().isOk());
		assertFalse(accounts.findByEmail(email).isEmpty());
	}

	@Test
	void registerAgainInCooldownIsRefused() throws Exception {
		register();
		mailedToken();
		register().andExpect(status().isTooManyRequests()).andExpect(jsonPath("$.msg").value("RESEND_TOO_SOON"))
				.andExpect(jsonPath("$.data.retryAfterSeconds").exists());
		verify(mailer, never()).send(anyString(), anyString(), anyString());
	}

	@Test
	void resendHasCooldownAndReplacesOldLink() throws Exception {
		register();
		String old = mailedToken();
		json("/api/auth/resend-verification", "{\"email\":\"" + email + "\"}")
				.andExpect(status().isTooManyRequests()).andExpect(jsonPath("$.msg").value("RESEND_TOO_SOON"));
		verify(mailer, never()).send(anyString(), anyString(), anyString());

		backdatePending();
		resend();
		String fresh = mailedToken();
		verifyWith(old).andExpect(status().isBadRequest());
		verifyWith(fresh).andExpect(status().isOk());
	}

	@Test
	void resendForUnknownEmailLooksTheSame() throws Exception {
		resend();
		verify(mailer, never()).send(anyString(), anyString(), anyString());
	}

	@Test
	void wrongTokenCreatesNoAccount() throws Exception {
		register();
		mailedToken();
		verifyWith("not-the-link").andExpect(status().isBadRequest());
		assertEquals(0, accounts.findAll().stream().filter(a -> email.equals(a.getEmail())).count());
	}
}
