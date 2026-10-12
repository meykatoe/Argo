package com.argo.customer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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

import com.argo.common.ApiException;
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

@SpringBootTest
@Transactional
class PasswordResetTests {

	static final String PW = "correct-horse-1";
	static final String NEW_PW = "brand-new-pass-2";

	@Autowired
	CustomerAuthService auth;
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
	String oldSession;

	@BeforeEach
	void setUp() {
		mvc = MockMvcBuilders.webAppContextSetup(wac).build();
		id = UUID.randomUUID().toString().substring(0, 8);
		username = "pr-" + id;
		email = "pr-" + id + "@example.com";
		oldSession = auth.register(username, email, PW, null).token();
		reset(mailer);
	}

	private ResultActions json(String path, String body) throws Exception {
		return mvc.perform(post(path).contentType(MediaType.APPLICATION_JSON).content(body));
	}

	private void forgot(String mail) throws Exception {
		json("/api/auth/forgot-password", "{\"email\":\"" + mail + "\"}").andExpect(status().isOk())
				.andExpect(jsonPath("$.code").value(200));
	}

	private String mailedToken() {
		ArgumentCaptor<String> body = ArgumentCaptor.forClass(String.class);
		verify(mailer, times(1)).send(eq(email), anyString(), body.capture());
		Matcher m = Pattern.compile("reset-password\\?token=([\\w-]+)").matcher(body.getValue());
		assertTrue(m.find());
		return m.group(1);
	}

	private ResultActions resetWith(String token, String password) throws Exception {
		return json("/api/auth/reset-password", "{\"token\":\"" + token + "\",\"password\":\"" + password + "\"}");
	}

	@Test
	void resetChangesPasswordAndEndsAllSessions() throws Exception {
		forgot(email);
		String t = mailedToken();
		resetWith(t, NEW_PW).andExpect(status().isOk());

		mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + oldSession))
				.andExpect(status().isUnauthorized());
		assertEquals("LOGIN_FAILED", assertThrows(ApiException.class, () -> auth.login(username, PW)).getCode());
		assertEquals(email, auth.login(username, NEW_PW).email());
		resetWith(t, NEW_PW).andExpect(status().isBadRequest()).andExpect(jsonPath("$.msg").value("TOKEN_INVALID"));
	}

	@Test
	void resetMarksEmailVerifiedAndClearsLock() throws Exception {
		for (int i = 0; i < 6; i++) {
			try {
				auth.login(username, "wrong-pass-9");
			} catch (ApiException e) {
				// 預期失敗
			}
		}
		assertEquals("LOGIN_LOCKED", assertThrows(ApiException.class, () -> auth.login(username, PW)).getCode());
		forgot(email);
		resetWith(mailedToken(), NEW_PW).andExpect(status().isOk());
		CustomerAuthView v = auth.login(username, NEW_PW);
		assertEquals(1, v.emailVerified());
	}

	@Test
	void unknownEmailLooksTheSameAndSendsNothing() throws Exception {
		forgot("nobody-" + id + "@example.com");
		verify(mailer, never()).send(anyString(), anyString(), anyString());
	}

	@Test
	void secondRequestInCooldownSendsNothing() throws Exception {
		forgot(email);
		forgot(email);
		mailedToken();
	}

	@Test
	void weakPasswordKeepsTokenUsable() throws Exception {
		forgot(email);
		String t = mailedToken();
		resetWith(t, "alllowercase").andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.data.password").value("validation.passwordWeak"));
		resetWith(t, NEW_PW).andExpect(status().isOk());
	}

	@Test
	void badAndExpiredTokensAreRejected() throws Exception {
		resetWith("nope", NEW_PW).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value(2008));
		forgot(email);
		String t = mailedToken();
		em.flush();
		jdbc.update("update customer_email_token set expires_at = now() - interval '1 minute' "
				+ "where customer_id = (select id from customer_account where email = ?)", email);
		em.clear();
		resetWith(t, NEW_PW).andExpect(status().isBadRequest());
	}

	@Test
	void verifyTokenCannotResetPassword() throws Exception {
		forgot(email);
		String t = mailedToken();
		json("/api/auth/verify-email", "{\"token\":\"" + t + "\"}").andExpect(status().isBadRequest());
	}

	@Test
	void disabledAccountGetsNoMail() throws Exception {
		em.flush();
		jdbc.update("update customer_account set disabled = 1 where email = ?", email);
		em.clear();
		forgot(email);
		verify(mailer, never()).send(anyString(), anyString(), anyString());
	}
}
