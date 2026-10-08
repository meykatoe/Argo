package com.argo.security.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.argo.security.block.AutoBlockMetric;
import com.argo.security.block.AutoBlockPolicyService;
import com.argo.security.block.IpBlockService;
import com.argo.staff.auth.LoginPortal;
import com.argo.staff.auth.StaffAuthService;
import com.argo.staff.auth.StaffRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
@Transactional
class OpsRulesApiTests {

	static final String PW = "Password-1234";

	@Autowired
	WebApplicationContext wac;
	@Autowired
	StaffAuthService auth;
	@Autowired
	JdbcTemplate jdbc;
	@Autowired
	AutoBlockPolicyService policy;
	@Autowired
	IpBlockService blocks;

	MockMvc mvc;
	String ops;

	@BeforeEach
	void setUp() {
		mvc = MockMvcBuilders.webAppContextSetup(wac).build();
		auth.create("ops1", PW, StaffRole.OPS);
		ops = "Bearer " + auth.login("ops1", PW, LoginPortal.OPS).token();
	}

	private ResultActions update(String metric, String body) throws Exception {
		return mvc.perform(patch("/api/ops/ip-rules/" + metric).header("Authorization", ops)
				.contentType(MediaType.APPLICATION_JSON).content(body));
	}

	private ResultActions allow(String body) throws Exception {
		return mvc.perform(post("/api/ops/ip-allowlist").header("Authorization", ops)
				.contentType(MediaType.APPLICATION_JSON).content(body));
	}

	@Test
	void onlyOpsCanSeeOrChangeRules() throws Exception {
		for (String role : new String[] { "ADMIN", "GENERAL", "SERVICE" }) {
			auth.create("y-" + role.toLowerCase(), PW, StaffRole.valueOf(role));
			String h = "Bearer " + auth.login("y-" + role.toLowerCase(), PW, LoginPortal.ADMIN).token();
			mvc.perform(get("/api/ops/ip-rules").header("Authorization", h)).andExpect(status().isForbidden());
			mvc.perform(patch("/api/ops/ip-rules/RATE_LIMITED").header("Authorization", h)
					.contentType(MediaType.APPLICATION_JSON).content("{\"enabled\":0,\"threshold\":5,\"windowMinutes\":5,\"blockHours\":1}"))
					.andExpect(status().isForbidden());
			mvc.perform(post("/api/ops/ip-allowlist").header("Authorization", h).contentType(MediaType.APPLICATION_JSON)
					.content("{\"ip\":\"203.0.113.140\",\"note\":\"x\"}")).andExpect(status().isForbidden());
		}
		mvc.perform(get("/api/ops/ip-rules")).andExpect(status().isUnauthorized());
	}

	@Test
	void listsTheTwoDefaultRulesAndTheAllowlist() throws Exception {
		allow("{\"ip\":\"203.0.113.141\",\"note\":\"辦公室\"}").andExpect(status().isOk());
		mvc.perform(get("/api/ops/ip-rules").header("Authorization", ops)).andExpect(status().isOk())
				.andExpect(jsonPath("$.data.rules.length()").value(2))
				.andExpect(jsonPath("$.data.rules[?(@.metric=='RATE_LIMITED')].enabled").value(1))
				.andExpect(jsonPath("$.data.allowlist[?(@.ip=='203.0.113.141')].note").value("辦公室"))
				.andExpect(jsonPath("$.data.allowlist[?(@.ip=='203.0.113.141')].createdBy").value("ops1"));
	}

	@Test
	void updatingARuleChangesItAtOnceAndIsAudited() throws Exception {
		update("LOGIN_FAILED", "{\"enabled\":1,\"threshold\":7,\"windowMinutes\":3,\"blockHours\":6}").andExpect(status().isOk())
				.andExpect(jsonPath("$.data.threshold").value(7)).andExpect(jsonPath("$.data.updatedBy").value("ops1"));
		var rule = policy.rule(AutoBlockMetric.LOGIN_FAILED);
		assertEquals(7, rule.threshold());
		assertEquals(3, rule.windowMinutes());
		assertEquals(6, rule.blockHours());
		var a = jdbc.queryForMap("select detail->>'before' b, detail->>'after' a, username from staff_audit_log where action = 'IP_RULE_UPDATED' and target_id = 'LOGIN_FAILED'");
		assertEquals("ops1", a.get("username"));
		assertEquals("on:40/10min->1h", a.get("b"));
		assertEquals("on:7/3min->6h", a.get("a"));
	}

	@Test
	void aRuleCanBeSwitchedOff() throws Exception {
		update("RATE_LIMITED", "{\"enabled\":0,\"threshold\":30,\"windowMinutes\":5,\"blockHours\":1}").andExpect(status().isOk())
				.andExpect(jsonPath("$.data.enabled").value(0));
		assertEquals(false, policy.rule(AutoBlockMetric.RATE_LIMITED).enabled());
	}

	@Test
	void rejectsSillyRuleValues() throws Exception {
		for (String body : new String[] {
				"{\"enabled\":2,\"threshold\":30,\"windowMinutes\":5,\"blockHours\":1}",
				"{\"enabled\":true,\"threshold\":30,\"windowMinutes\":5,\"blockHours\":1}",
				"{\"enabled\":1,\"threshold\":1,\"windowMinutes\":5,\"blockHours\":1}",
				"{\"enabled\":1,\"threshold\":30,\"windowMinutes\":0,\"blockHours\":1}",
				"{\"enabled\":1,\"threshold\":30,\"windowMinutes\":1441,\"blockHours\":1}",
				"{\"enabled\":1,\"threshold\":30,\"windowMinutes\":5,\"blockHours\":0}",
				"{\"enabled\":1,\"threshold\":30,\"windowMinutes\":5,\"blockHours\":9000}",
				"{\"threshold\":30,\"windowMinutes\":5,\"blockHours\":1}" }) {
			update("RATE_LIMITED", body).andExpect(status().isBadRequest());
		}
		update("NOT_A_METRIC", "{\"enabled\":1,\"threshold\":30,\"windowMinutes\":5,\"blockHours\":1}").andExpect(status().isBadRequest());
		assertEquals(30, policy.rule(AutoBlockMetric.RATE_LIMITED).threshold());
	}

	@Test
	void allowlistAcceptsIpsAndReasonableCidrsInOneSpelling() throws Exception {
		allow("{\"ip\":\"2001:db8::9\",\"note\":\"v6\"}").andExpect(status().isOk())
				.andExpect(jsonPath("$.data.ip").value("2001:db8:0:0:0:0:0:9"));
		allow("{\"ip\":\"203.0.113.0/24\",\"note\":\"分校\"}").andExpect(status().isOk())
				.andExpect(jsonPath("$.data.ip").value("203.0.113.0/24"));
		assertEquals(true, policy.isAllowed("203.0.113.55"));
		assertEquals(true, policy.isAllowed("2001:db8::9"));
		assertEquals(false, policy.isAllowed("203.0.114.55"));
		assertEquals(1, jdbc.queryForObject("select count(*) from staff_audit_log where action = 'IP_ALLOWLIST_ADDED' and target_id = '203.0.113.0/24'", Integer.class));
	}

	@Test
	void allowlistRefusesNonsenseAndHugeRanges() throws Exception {
		for (String ip : new String[] { "example.com", "999.1.1.1", "0.0.0.0/0", "10.0.0.0/7", "::/0", "2001:db8::/16", "1.2.3.4/33", "1.2.3.4/x" }) {
			allow("{\"ip\":\"" + ip + "\",\"note\":\"x\"}").andExpect(status().isBadRequest()).andExpect(jsonPath("$.msg").value("INVALID_IP"));
		}
		allow("{\"ip\":\"203.0.113.142\",\"note\":\"\"}").andExpect(status().isBadRequest());
		allow("{\"note\":\"x\"}").andExpect(status().isBadRequest());
	}

	@Test
	void removingAnAllowEntryWorksAndUnknownIs404() throws Exception {
		allow("{\"ip\":\"203.0.113.143\",\"note\":\"x\"}").andExpect(status().isOk());
		mvc.perform(delete("/api/ops/ip-allowlist").param("ip", "203.0.113.143").header("Authorization", ops)).andExpect(status().isOk());
		assertEquals(false, policy.isAllowed("203.0.113.143"));
		assertEquals(1, jdbc.queryForObject("select count(*) from staff_audit_log where action = 'IP_ALLOWLIST_REMOVED' and target_id = '203.0.113.143'", Integer.class));
		mvc.perform(delete("/api/ops/ip-allowlist").param("ip", "203.0.113.143").header("Authorization", ops)).andExpect(status().isNotFound());
		mvc.perform(delete("/api/ops/ip-allowlist").param("ip", "junk").header("Authorization", ops)).andExpect(status().isBadRequest());
	}

	@Test
	void autoBlocksAreMarkedInTheBlockListAndActivityList() throws Exception {
		blocks.autoBlock("198.51.100.150", "自動封鎖：測試", 1, java.util.Map.of("metric", "RATE_LIMITED"));
		jdbc.update("insert into ip_activity (ip, day, rate_limited, login_failed, blocked_hits, first_seen, last_seen) values ('198.51.100.150', current_date, 31, 0, 0, now(), now())");
		mvc.perform(get("/api/ops/ip-blocks").header("Authorization", ops))
				.andExpect(jsonPath("$.data[?(@.ip=='198.51.100.150')].auto").value(1))
				.andExpect(jsonPath("$.data[?(@.ip=='198.51.100.150')].blockedBy").value("system:auto-block"));
		mvc.perform(get("/api/ops/ips").param("keyword", "198.51.100.150").header("Authorization", ops))
				.andExpect(jsonPath("$.data.items[0].blocked").value(1))
				.andExpect(jsonPath("$.data.items[0].blockAuto").value(1));
	}

	@Test
	void anOperatorCanTurnAnAutoBlockIntoAPermanentOne() throws Exception {
		blocks.autoBlock("198.51.100.151", "自動封鎖：測試", 1, java.util.Map.of("metric", "LOGIN_FAILED"));
		mvc.perform(post("/api/ops/ip-blocks").header("Authorization", ops).contentType(MediaType.APPLICATION_JSON)
				.content("{\"ip\":\"198.51.100.151\",\"reason\":\"確認為惡意，轉為永久\"}")).andExpect(status().isOk())
				.andExpect(jsonPath("$.data.auto").value(0)).andExpect(jsonPath("$.data.expiresAt").doesNotExist());
		assertEquals("ops1", jdbc.queryForObject("select blocked_by from ip_block where ip = '198.51.100.151'", String.class));
		assertEquals(0, jdbc.queryForObject("select auto from ip_block where ip = '198.51.100.151'", Integer.class));
	}

	@Test
	void opsMenuGainsTheRulesEntries() throws Exception {
		mvc.perform(get("/api/ops/menu").header("Authorization", ops)).andExpect(status().isOk())
				.andExpect(jsonPath("$.data[1].children[2].code").value("security.rules"))
				.andExpect(jsonPath("$.data[1].children[2].path").value("/ip-rules"))
				.andExpect(jsonPath("$.data[1].children[3].code").value("security.rules.edit"));
	}
}
