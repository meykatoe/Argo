package com.argo.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
class OpsSecurityApiTests {

	static final String PW = "password-1234";

	@Autowired
	WebApplicationContext wac;
	@Autowired
	StaffAuthService auth;
	@Autowired
	IpBlockService blocks;
	@Autowired
	JdbcTemplate jdbc;

	MockMvc mvc;
	String ops;

	@BeforeEach
	void setUp() {
		mvc = MockMvcBuilders.webAppContextSetup(wac).build();
		auth.create("ops1", PW, StaffRole.OPS);
		ops = "Bearer " + auth.login("ops1", PW, LoginPortal.OPS).token();
	}

	private ResultActions block(String body) throws Exception {
		return mvc.perform(post("/api/ops/ip-blocks").header("Authorization", ops)
				.contentType(MediaType.APPLICATION_JSON).content(body));
	}

	private void seed(String ip, int days, int rl, int lf, int bh) {
		jdbc.update("insert into ip_activity (ip, day, rate_limited, login_failed, blocked_hits, first_seen, last_seen) "
				+ "values (?, current_date - ?, ?, ?, ?, now() - interval '1 hour', now())", ip, days, rl, lf, bh);
	}

	@Test
	void onlyOpsCanSeeOrChangeAnything() throws Exception {
		for (String role : new String[] { "ADMIN", "GENERAL", "SERVICE" }) {
			auth.create("x-" + role.toLowerCase(), PW, StaffRole.valueOf(role));
			String h = "Bearer " + auth.login("x-" + role.toLowerCase(), PW, LoginPortal.ADMIN).token();
			mvc.perform(get("/api/ops/ips").header("Authorization", h)).andExpect(status().isForbidden());
			mvc.perform(get("/api/ops/ip-blocks").header("Authorization", h)).andExpect(status().isForbidden());
			mvc.perform(post("/api/ops/ip-blocks").header("Authorization", h).contentType(MediaType.APPLICATION_JSON)
					.content("{\"ip\":\"203.0.113.50\",\"reason\":\"x\"}")).andExpect(status().isForbidden());
			mvc.perform(delete("/api/ops/ip-blocks").param("ip", "203.0.113.50").header("Authorization", h))
					.andExpect(status().isForbidden());
		}
		mvc.perform(get("/api/ops/ips")).andExpect(status().isUnauthorized());
		assertEquals(0, jdbc.queryForObject("select count(*) from ip_block where ip = '203.0.113.50'", Integer.class));
	}

	@Test
	void blocksAndUnblocksWithAnAuditTrail() throws Exception {
		block("{\"ip\":\"203.0.113.51\",\"reason\":\"猜密碼\"}").andExpect(status().isOk())
				.andExpect(jsonPath("$.data.ip").value("203.0.113.51"))
				.andExpect(jsonPath("$.data.blockedBy").value("ops1"))
				.andExpect(jsonPath("$.data.expiresAt").doesNotExist());
		assertEquals(true, blocks.isBlocked("203.0.113.51"));
		mvc.perform(get("/api/ops/ip-blocks").header("Authorization", ops)).andExpect(status().isOk())
				.andExpect(jsonPath("$.data[?(@.ip=='203.0.113.51')].reason").value("猜密碼"));

		mvc.perform(delete("/api/ops/ip-blocks").param("ip", "203.0.113.51").header("Authorization", ops))
				.andExpect(status().isOk());
		assertEquals(false, blocks.isBlocked("203.0.113.51"));

		assertEquals(1, jdbc.queryForObject("select count(*) from staff_audit_log where action = 'IP_BLOCKED' and target_id = '203.0.113.51' and username = 'ops1' and detail->>'reason' = '猜密碼'", Integer.class));
		assertEquals(1, jdbc.queryForObject("select count(*) from staff_audit_log where action = 'IP_UNBLOCKED' and target_id = '203.0.113.51' and username = 'ops1'", Integer.class));
	}

	@Test
	void temporaryBlockHasAnExpiry() throws Exception {
		block("{\"ip\":\"203.0.113.52\",\"reason\":\"短期\",\"hours\":2}").andExpect(status().isOk())
				.andExpect(jsonPath("$.data.expiresAt").exists());
		assertEquals("2", jdbc.queryForObject("select detail->>'hours' from staff_audit_log where action = 'IP_BLOCKED' and target_id = '203.0.113.52'", String.class));
	}

	@Test
	void blockingTheSameIpAgainReplacesTheOldRecord() throws Exception {
		block("{\"ip\":\"203.0.113.53\",\"reason\":\"第一次\",\"hours\":1}").andExpect(status().isOk());
		block("{\"ip\":\"203.0.113.53\",\"reason\":\"改為永久\"}").andExpect(status().isOk());
		assertEquals(1, jdbc.queryForObject("select count(*) from ip_block where ip = '203.0.113.53'", Integer.class));
		assertEquals("改為永久", jdbc.queryForObject("select reason from ip_block where ip = '203.0.113.53'", String.class));
	}

	@Test
	void equivalentSpellingsOfTheSameIpAreOneBlock() throws Exception {
		block("{\"ip\":\"2001:db8::7\",\"reason\":\"v6\"}").andExpect(status().isOk());
		assertEquals(true, blocks.isBlocked("2001:db8:0:0:0:0:0:7"));
		mvc.perform(delete("/api/ops/ip-blocks").param("ip", "2001:0db8:0000:0000:0000:0000:0000:0007").header("Authorization", ops))
				.andExpect(status().isOk());
		assertEquals(false, blocks.isBlocked("2001:db8:0:0:0:0:0:7"));
	}

	@Test
	void refusesDangerousOrInvalidTargets() throws Exception {
		for (String ip : new String[] { "127.0.0.1", "::1", "0.0.0.0", "169.254.0.5", "224.0.0.9" }) {
			block("{\"ip\":\"" + ip + "\",\"reason\":\"x\"}").andExpect(status().isBadRequest())
					.andExpect(jsonPath("$.msg").value("PROTECTED_IP"));
		}
		for (String ip : new String[] { "example.com", "999.1.1.1", "10.0.0.0/8", "not an ip" }) {
			block("{\"ip\":\"" + ip + "\",\"reason\":\"x\"}").andExpect(status().isBadRequest())
					.andExpect(jsonPath("$.msg").value("INVALID_IP"));
		}
		assertEquals(0, jdbc.queryForObject("select count(*) from ip_block where blocked_by = 'ops1'", Integer.class));
	}

	@Test
	void cannotBlockYourOwnAddress() throws Exception {
		// 本機是受信任代理，所以以轉送標頭當作操作者的真實 IP
		mvc.perform(post("/api/ops/ip-blocks").header("Authorization", ops).header("X-Forwarded-For", "203.0.113.99")
				.contentType(MediaType.APPLICATION_JSON).content("{\"ip\":\"203.0.113.99\",\"reason\":\"x\"}"))
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.msg").value("CANNOT_BLOCK_SELF"));
	}

	@Test
	void validatesTheRequestBody() throws Exception {
		block("{\"ip\":\"203.0.113.54\"}").andExpect(status().isBadRequest()).andExpect(jsonPath("$.msg").value("VALIDATION_ERROR"));
		block("{\"ip\":\"203.0.113.54\",\"reason\":\"  \"}").andExpect(status().isBadRequest());
		block("{\"reason\":\"x\"}").andExpect(status().isBadRequest());
		block("{\"ip\":\"203.0.113.54\",\"reason\":\"x\",\"hours\":0}").andExpect(status().isBadRequest());
		block("{\"ip\":\"203.0.113.54\",\"reason\":\"x\",\"hours\":9000}").andExpect(status().isBadRequest());
		block("{\"ip\":\"203.0.113.54\",\"reason\":\"" + "長".repeat(201) + "\"}").andExpect(status().isBadRequest());
	}

	@Test
	void unblockingAnUnknownIpIs404() throws Exception {
		mvc.perform(delete("/api/ops/ip-blocks").param("ip", "203.0.113.55").header("Authorization", ops))
				.andExpect(status().isNotFound()).andExpect(jsonPath("$.msg").value("IP_NOT_BLOCKED"));
		mvc.perform(delete("/api/ops/ip-blocks").param("ip", "garbage").header("Authorization", ops))
				.andExpect(status().isBadRequest());
	}

	@Test
	void expiredBlocksAreNotListed() throws Exception {
		block("{\"ip\":\"203.0.113.56\",\"reason\":\"短期\",\"hours\":1}").andExpect(status().isOk());
		jdbc.update("update ip_block set expires_at = now() - interval '1 minute' where ip = '203.0.113.56'");
		mvc.perform(get("/api/ops/ip-blocks").header("Authorization", ops))
				.andExpect(jsonPath("$.data[?(@.ip=='203.0.113.56')]").isEmpty());
	}

	@Test
	void activityListSumsDaysSortsByTroubleAndShowsBlockState() throws Exception {
		seed("198.51.100.60", 0, 5, 1, 0);
		seed("198.51.100.60", 1, 3, 0, 0);
		seed("198.51.100.61", 0, 1, 0, 0);
		seed("198.51.100.62", 0, 0, 20, 4);
		block("{\"ip\":\"198.51.100.62\",\"reason\":\"撞庫\"}").andExpect(status().isOk());
		mvc.perform(get("/api/ops/ips").param("keyword", "198.51.100.6").header("Authorization", ops))
				.andExpect(status().isOk()).andExpect(jsonPath("$.data.total").value(3))
				.andExpect(jsonPath("$.data.items[0].ip").value("198.51.100.62"))
				.andExpect(jsonPath("$.data.items[0].blocked").value(1))
				.andExpect(jsonPath("$.data.items[0].blockReason").value("撞庫"))
				.andExpect(jsonPath("$.data.items[0].loginFailed").value(20))
				.andExpect(jsonPath("$.data.items[1].ip").value("198.51.100.60"))
				.andExpect(jsonPath("$.data.items[1].rateLimited").value(8))
				.andExpect(jsonPath("$.data.items[1].blocked").value(0))
				.andExpect(jsonPath("$.data.items[2].ip").value("198.51.100.61"));
	}

	@Test
	void daysWindowLimitsWhatIsCounted() throws Exception {
		seed("198.51.100.70", 5, 9, 0, 0);
		mvc.perform(get("/api/ops/ips").param("keyword", "198.51.100.70").param("days", "3").header("Authorization", ops))
				.andExpect(jsonPath("$.data.total").value(0));
		mvc.perform(get("/api/ops/ips").param("keyword", "198.51.100.70").param("days", "7").header("Authorization", ops))
				.andExpect(jsonPath("$.data.total").value(1)).andExpect(jsonPath("$.data.items[0].rateLimited").value(9));
	}

	@Test
	void activityPaginationAndParametersAreValidated() throws Exception {
		for (int i = 0; i < 3; i++) {
			seed("198.51.100." + (80 + i), 0, 1 + i, 0, 0);
		}
		mvc.perform(get("/api/ops/ips").param("keyword", "198.51.100.8").param("size", "2").header("Authorization", ops))
				.andExpect(jsonPath("$.data.total").value(3)).andExpect(jsonPath("$.data.totalPages").value(2))
				.andExpect(jsonPath("$.data.items.length()").value(2));
		mvc.perform(get("/api/ops/ips").param("keyword", "198.51.100.8").param("size", "2").param("page", "2").header("Authorization", ops))
				.andExpect(jsonPath("$.data.items.length()").value(1));
		for (String q : new String[] { "days=0", "days=31", "page=0", "size=0", "size=101", "keyword=%27%3B+drop" }) {
			mvc.perform(get("/api/ops/ips?" + q).header("Authorization", ops)).andExpect(status().isBadRequest());
		}
	}

	@Test
	void opsMenuNowIncludesTheSecurityEntriesAndTheBlockCapability() throws Exception {
		mvc.perform(get("/api/ops/menu").header("Authorization", ops)).andExpect(status().isOk())
				.andExpect(jsonPath("$.data[1].code").value("security"))
				.andExpect(jsonPath("$.data[1].children[0].code").value("security.ips"))
				.andExpect(jsonPath("$.data[1].children[0].path").value("/ips"))
				.andExpect(jsonPath("$.data[1].children[1].code").value("security.ips.block"));
	}
}
