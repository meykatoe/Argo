package com.argo.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.argo.admin.LoginPortal;
import com.argo.admin.StaffAuthService;
import com.argo.admin.StaffRole;
import java.time.OffsetDateTime;
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

@SpringBootTest
@Transactional
@TestPropertySource(properties = { "argo.ratelimit.general-per-minute=5", "argo.ratelimit.auth-per-minute=3",
		"argo.ratelimit.checkout-per-minute=2" })
class IpGuardFilterTests {

	static final String LOGIN = "{\"email\":\"nobody@test.local\",\"password\":\"wrong-password\"}";

	@Autowired
	WebApplicationContext wac;
	@Autowired
	IpGuardFilter filter;
	@Autowired
	IpBlockService blocks;
	@Autowired
	IpActivityRecorder recorder;
	@Autowired
	StaffAuthService staff;
	@Autowired
	JdbcTemplate jdbc;

	MockMvc mvc;

	@BeforeEach
	void setUp() {
		mvc = MockMvcBuilders.webAppContextSetup(wac).addFilters(filter).build();
	}

	// 模擬直接連線的用戶端
	private static MockHttpServletRequestBuilder from(MockHttpServletRequestBuilder b, String ip) {
		return b.with(r -> {
			r.setRemoteAddr(ip);
			return r;
		});
	}

	private MockHttpServletRequestBuilder ping(String ip) {
		return from(get("/api/ping"), ip);
	}

	private MockHttpServletRequestBuilder login(String ip) {
		return from(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(LOGIN), ip);
	}

	private void blockNow(String ip) {
		staff.create("op-" + ip.replace('.', '-'), "password-1234", StaffRole.OPS);
		var actor = staff.login("op-" + ip.replace('.', '-'), "password-1234", LoginPortal.OPS);
		var acc = jdbc.queryForObject("select id from staff_account where username = ?", Long.class, actor.username());
		// 直接寫入，避免依賴運維 API
		jdbc.update("insert into ip_block (ip, reason, blocked_by, staff_id) values (?, 'test', 'tester', ?)", ip, acc);
		blocks.refreshNow();
	}

	private int activity(String ip, String column) {
		recorder.flush();
		Integer n = jdbc.queryForObject("select coalesce(sum(" + column + "), 0) from ip_activity where ip = ?", Integer.class, ip);
		return n == null ? 0 : n;
	}

	// 沒寫入的計數會在關閉時被真的提交，所以每個測試結束前先在交易內寫掉，隨交易一起回滾
	@AfterEach
	void drain() {
		recorder.flush();
	}

	@Test
	void normalTrafficPasses() throws Exception {
		mvc.perform(ping("203.0.113.10")).andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
	}

	@Test
	void blockedIpGets403ForEveryApiCall() throws Exception {
		blockNow("203.0.113.11");
		mvc.perform(ping("203.0.113.11")).andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value(6001)).andExpect(jsonPath("$.msg").value("IP_BLOCKED"));
		mvc.perform(from(get("/api/cards"), "203.0.113.11")).andExpect(status().isForbidden());
		mvc.perform(from(get("/api/admin/menu"), "203.0.113.11")).andExpect(status().isForbidden());
		mvc.perform(login("203.0.113.11")).andExpect(status().isForbidden());
		// 別的 IP 不受影響
		mvc.perform(ping("203.0.113.12")).andExpect(status().isOk());
		assertEquals(4, activity("203.0.113.11", "blocked_hits"));
	}

	@Test
	void unblockingRestoresAccessImmediately() throws Exception {
		blockNow("203.0.113.13");
		mvc.perform(ping("203.0.113.13")).andExpect(status().isForbidden());
		jdbc.update("delete from ip_block where ip = '203.0.113.13'");
		blocks.refreshNow();
		mvc.perform(ping("203.0.113.13")).andExpect(status().isOk());
	}

	@Test
	void expiredBlocksNoLongerApply() throws Exception {
		blockNow("203.0.113.14");
		jdbc.update("update ip_block set expires_at = now() - interval '1 minute' where ip = '203.0.113.14'");
		blocks.refreshNow();
		mvc.perform(ping("203.0.113.14")).andExpect(status().isOk());
	}

	@Test
	void futureExpiryStillBlocks() throws Exception {
		blockNow("203.0.113.15");
		jdbc.update("update ip_block set expires_at = ? where ip = '203.0.113.15'", OffsetDateTime.now().plusHours(1));
		blocks.refreshNow();
		mvc.perform(ping("203.0.113.15")).andExpect(status().isForbidden());
	}

	@Test
	void rateLimitReturns429WithRetryInfo() throws Exception {
		for (int i = 0; i < 5; i++) {
			mvc.perform(ping("203.0.113.16")).andExpect(status().isOk());
		}
		mvc.perform(ping("203.0.113.16")).andExpect(status().isTooManyRequests())
				.andExpect(jsonPath("$.code").value(6002)).andExpect(jsonPath("$.msg").value("RATE_LIMITED"))
				.andExpect(jsonPath("$.data.retryAfterSeconds").exists()).andExpect(header().exists("Retry-After"));
		mvc.perform(ping("203.0.113.17")).andExpect(status().isOk());
		assertEquals(1, activity("203.0.113.16", "rate_limited"));
	}

	@Test
	void loginAttemptsHaveTheirOwnStricterLimitAndFailuresAreRecorded() throws Exception {
		for (int i = 0; i < 3; i++) {
			mvc.perform(login("203.0.113.18")).andExpect(status().isUnauthorized());
		}
		mvc.perform(login("203.0.113.18")).andExpect(status().isTooManyRequests())
				.andExpect(jsonPath("$.msg").value("RATE_LIMITED"));
		// 一般瀏覽仍可用
		mvc.perform(ping("203.0.113.18")).andExpect(status().isOk());
		assertEquals(3, activity("203.0.113.18", "login_failed"));
		assertEquals(1, activity("203.0.113.18", "rate_limited"));
	}

	@Test
	void checkoutCallsAreLimitedSeparately() throws Exception {
		for (int i = 0; i < 2; i++) {
			mvc.perform(from(post("/api/orders").contentType(MediaType.APPLICATION_JSON).content("{}"), "203.0.113.19"))
					.andExpect(status().isBadRequest());
		}
		mvc.perform(from(post("/api/orders").contentType(MediaType.APPLICATION_JSON).content("{}"), "203.0.113.19"))
				.andExpect(status().isTooManyRequests());
		mvc.perform(from(post("/api/orders/AR1-ABC/pay").contentType(MediaType.APPLICATION_JSON).content("{}"), "203.0.113.19"))
				.andExpect(status().isTooManyRequests());
	}

	@Test
	void usesTheForwardedClientAddressOnlyFromTrustedProxies() throws Exception {
		blockNow("203.0.113.20");
		// 受信任的本機代理轉送：以標頭中的真實 IP 為準
		mvc.perform(ping("127.0.0.1").header("X-Forwarded-For", "203.0.113.20")).andExpect(status().isForbidden());
		mvc.perform(ping("127.0.0.1").header("X-Forwarded-For", "203.0.113.21")).andExpect(status().isOk());
		// 不受信任的來源亂填標頭沒有用，也不能害別人被擋
		mvc.perform(ping("198.51.100.30").header("X-Forwarded-For", "203.0.113.20")).andExpect(status().isOk());
		mvc.perform(ping("203.0.113.20").header("X-Forwarded-For", "198.51.100.31")).andExpect(status().isForbidden());
	}

	@Test
	void rejectionsCarryCorsHeadersOnlyForAllowedOrigins() throws Exception {
		blockNow("203.0.113.22");
		mvc.perform(ping("203.0.113.22").header("Origin", "http://localhost:5173")).andExpect(status().isForbidden())
				.andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
		var r = mvc.perform(ping("203.0.113.22").header("Origin", "http://evil.example")).andExpect(status().isForbidden()).andReturn();
		assertNull(r.getResponse().getHeader("Access-Control-Allow-Origin"));
	}

	@Test
	void preflightRequestsAreNotCounted() throws Exception {
		for (int i = 0; i < 20; i++) {
			mvc.perform(from(options("/api/cards").header("Origin", "http://localhost:5173")
					.header("Access-Control-Request-Method", "GET"), "203.0.113.23")).andExpect(status().isOk());
		}
		mvc.perform(ping("203.0.113.23")).andExpect(status().isOk());
	}

	@Test
	void onlyApiPathsAreGuarded() throws Exception {
		blockNow("203.0.113.24");
		mvc.perform(from(get("/not-api"), "203.0.113.24")).andExpect(status().isNotFound());
	}
}
