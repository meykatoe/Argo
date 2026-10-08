package com.argo.security.guard;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.argo.security.block.AutoBlockPolicyService;
import com.argo.security.block.IpBlockService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

// 從請求進來到自動封鎖生效的完整流程
@SpringBootTest
@Transactional
@TestPropertySource(properties = { "argo.ratelimit.general-per-minute=4", "argo.ratelimit.auth-per-minute=3",
		"argo.ratelimit.checkout-per-minute=30" })
class AutoBlockFilterTests {

	@Autowired
	WebApplicationContext wac;
	@Autowired
	IpGuardFilter filter;
	@Autowired
	AutoBlockPolicyService policy;
	@Autowired
	IpBlockService blocks;
	@Autowired
	IpActivityRecorder recorder;
	@Autowired
	JdbcTemplate jdbc;
	@jakarta.persistence.PersistenceContext
	jakarta.persistence.EntityManager em;

	MockMvc mvc;

	@BeforeEach
	void setUp() {
		mvc = MockMvcBuilders.webAppContextSetup(wac).addFilters(filter).build();
		jdbc.update("update ip_auto_block_rule set enabled = 1, threshold = 3, window_minutes = 5, block_hours = 1 where metric = 'RATE_LIMITED'");
		jdbc.update("update ip_auto_block_rule set enabled = 1, threshold = 3, window_minutes = 5, block_hours = 1 where metric = 'LOGIN_FAILED'");
		em.clear();
		policy.refreshNow();
	}

	@AfterEach
	void drain() {
		recorder.flush();
	}

	private static MockHttpServletRequestBuilder from(MockHttpServletRequestBuilder b, String ip) {
		return b.with(r -> {
			r.setRemoteAddr(ip);
			return r;
		});
	}

	private MockHttpServletRequestBuilder ping(String ip) {
		return from(get("/api/ping"), ip);
	}

	@Test
	void aFloodGetsAutoBlockedAndThenCutOff() throws Exception {
		String ip = "203.0.113.131";
		for (int i = 0; i < 4; i++) {
			mvc.perform(ping(ip)).andExpect(status().isOk());
		}
		// 之後每一次都被限速，累積 3 次就自動封鎖
		for (int i = 0; i < 3; i++) {
			mvc.perform(ping(ip)).andExpect(status().isTooManyRequests());
		}
		mvc.perform(ping(ip)).andExpect(status().isForbidden()).andExpect(jsonPath("$.msg").value("IP_BLOCKED"));
		assertEquals(1, jdbc.queryForObject("select auto from ip_block where ip = ?", Integer.class, ip));
		assertEquals("system:auto-block", jdbc.queryForObject("select blocked_by from ip_block where ip = ?", String.class, ip));
		// 別人不受影響
		mvc.perform(ping("203.0.113.132")).andExpect(status().isOk());
	}

	@Test
	void repeatedLoginFailuresGetAutoBlocked() throws Exception {
		String ip = "203.0.113.133";
		String body = "{\"account\":\"nobody@test.local\",\"password\":\"wrong-password\"}";
		for (int i = 0; i < 3; i++) {
			mvc.perform(from(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(body), ip))
					.andExpect(status().isUnauthorized());
		}
		mvc.perform(ping(ip)).andExpect(status().isForbidden());
		assertEquals("LOGIN_FAILED", jdbc.queryForObject("select detail->>'metric' from staff_audit_log where action = 'IP_BLOCKED' and target_id = ?", String.class, ip));
	}

	@Test
	void anAllowlistedIpIsRateLimitedButNeverBlocked() throws Exception {
		String ip = "203.0.113.134";
		jdbc.update("insert into ip_allowlist (ip, note, created_by) values (?, '辦公室', 'tester')", ip);
		em.clear();
		policy.refreshNow();
		for (int i = 0; i < 20; i++) {
			mvc.perform(ping(ip));
		}
		mvc.perform(ping(ip)).andExpect(status().isTooManyRequests());
		assertEquals(0, jdbc.queryForObject("select count(*) from ip_block where ip = ?", Integer.class, ip));
	}

	@Test
	void forwardedAddressesCannotBeUsedToGetSomeoneElseBlocked() throws Exception {
		String victim = "203.0.113.135";
		// 攻擊者直接連線卻偽造受害者的位址，被算在攻擊者自己身上
		for (int i = 0; i < 12; i++) {
			mvc.perform(from(get("/api/ping").header("X-Forwarded-For", victim), "198.51.100.136"));
		}
		mvc.perform(ping("198.51.100.136")).andExpect(status().isForbidden());
		mvc.perform(ping(victim)).andExpect(status().isOk());
		assertEquals(0, jdbc.queryForObject("select count(*) from ip_block where ip = ?", Integer.class, victim));
	}

	@Test
	void disablingTheRuleStopsAutoBlocking() throws Exception {
		jdbc.update("update ip_auto_block_rule set enabled = 0 where metric = 'RATE_LIMITED'");
		em.clear();
		policy.refreshNow();
		String ip = "203.0.113.137";
		for (int i = 0; i < 15; i++) {
			mvc.perform(ping(ip));
		}
		mvc.perform(ping(ip)).andExpect(status().isTooManyRequests());
	}
}
