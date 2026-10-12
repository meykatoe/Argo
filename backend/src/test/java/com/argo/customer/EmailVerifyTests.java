package com.argo.customer;

import static org.junit.jupiter.api.Assertions.assertTrue;
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
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
@Transactional
class EmailVerifyTests {

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
	@MockitoBean
	Mailer mailer;

	MockMvc mvc;
	String email;
	String token;

	@BeforeEach
	void setUp() {
		mvc = MockMvcBuilders.webAppContextSetup(wac).build();
		reset(mailer);
		String id = UUID.randomUUID().toString().substring(0, 8);
		email = "ev-" + id + "@example.com";
		token = auth.register("ev-" + id, email, PW, null).token();
	}

	private String mailedToken() {
		ArgumentCaptor<String> body = ArgumentCaptor.forClass(String.class);
		verify(mailer, times(1)).send(eq(email), org.mockito.ArgumentMatchers.anyString(), body.capture());
		Matcher m = Pattern.compile("token=([\\w-]+)").matcher(body.getValue());
		assertTrue(m.find());
		return m.group(1);
	}

	private void verifyBody(String t, int expectStatus) throws Exception {
		mvc.perform(post("/api/auth/verify-email").contentType(MediaType.APPLICATION_JSON)
				.content("{\"token\":\"" + t + "\"}")).andExpect(status().is(expectStatus));
	}

	private void expectVerified(int flag) throws Exception {
		mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token)).andExpect(status().isOk())
				.andExpect(jsonPath("$.data.emailVerified").value(flag));
	}

	@Test
	void registerMailsLinkAndStartsUnverified() throws Exception {
		mailedToken();
		expectVerified(0);
	}

	@Test
	void linkVerifiesOnceOnly() throws Exception {
		String t = mailedToken();
		verifyBody(t, 200);
		expectVerified(1);
		verifyBody(t, 400);
		mvc.perform(post("/api/auth/verify-email").contentType(MediaType.APPLICATION_JSON)
				.content("{\"token\":\"" + t + "\"}")).andExpect(jsonPath("$.msg").value("TOKEN_INVALID"))
				.andExpect(jsonPath("$.code").value(2008));
	}

	@Test
	void unknownAndExpiredTokensAreRejected() throws Exception {
		verifyBody("nope", 400);
		String t = mailedToken();
		em.flush();
		jdbc.update("update customer_email_token set expires_at = now() - interval '1 minute' "
				+ "where customer_id = (select id from customer_account where email = ?)", email);
		em.clear();
		verifyBody(t, 400);
		expectVerified(0);
	}

	@Test
	void resendHasCooldownAndReplacesOldLink() throws Exception {
		String old = mailedToken();
		reset(mailer);
		mvc.perform(post("/api/auth/resend-verification").header("Authorization", "Bearer " + token))
				.andExpect(status().isTooManyRequests()).andExpect(jsonPath("$.msg").value("RESEND_TOO_SOON"))
				.andExpect(jsonPath("$.data.retryAfterSeconds").exists());
		verify(mailer, never()).send(eq(email), org.mockito.ArgumentMatchers.anyString(),
				org.mockito.ArgumentMatchers.anyString());

		em.flush();
		jdbc.update("update customer_email_token set created_at = now() - interval '5 minutes' "
				+ "where customer_id = (select id from customer_account where email = ?)", email);
		em.clear();
		mvc.perform(post("/api/auth/resend-verification").header("Authorization", "Bearer " + token))
				.andExpect(status().isOk());
		String fresh = mailedToken();
		verifyBody(old, 400);
		verifyBody(fresh, 200);
		expectVerified(1);
	}

	@Test
	void resendNeedsLoginAndSkipsVerified() throws Exception {
		mvc.perform(post("/api/auth/resend-verification")).andExpect(status().isUnauthorized());
		verifyBody(mailedToken(), 200);
		reset(mailer);
		mvc.perform(post("/api/auth/resend-verification").header("Authorization", "Bearer " + token))
				.andExpect(status().isOk());
		verify(mailer, never()).send(eq(email), org.mockito.ArgumentMatchers.anyString(),
				org.mockito.ArgumentMatchers.anyString());
	}
}
