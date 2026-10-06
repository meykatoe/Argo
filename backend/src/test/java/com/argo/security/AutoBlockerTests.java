package com.argo.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class AutoBlockerTests {

	@Autowired
	AutoBlockPolicyService policy;
	@Autowired
	IpBlockService blocks;
	@Autowired
	ClientIpResolver resolver;
	@Autowired
	JdbcTemplate jdbc;
	@jakarta.persistence.PersistenceContext
	jakarta.persistence.EntityManager em;

	final AtomicLong now = new AtomicLong(1_000_000);
	AutoBlocker blocker;

	@BeforeEach
	void setUp() {
		blocker = new AutoBlocker(policy, blocks, resolver, now::get);
		// 測試用規則：2 分鐘內 3 次，封鎖 2 小時
		jdbc.update("update ip_auto_block_rule set enabled = 1, threshold = 3, window_minutes = 2, block_hours = 2 where metric = 'RATE_LIMITED'");
		jdbc.update("update ip_auto_block_rule set enabled = 1, threshold = 4, window_minutes = 5, block_hours = 1 where metric = 'LOGIN_FAILED'");
		em.clear();
		policy.refreshNow();
	}

	private void hit(String ip, AutoBlockMetric m, int times) {
		for (int i = 0; i < times; i++) {
			blocker.record(ip, m);
			now.addAndGet(1_000);
		}
	}

	private int audits(String ip) {
		return jdbc.queryForObject("select count(*) from staff_audit_log where action = 'IP_BLOCKED' and target_id = ?", Integer.class, ip);
	}

	@Test
	void belowTheThresholdNothingHappens() {
		hit("198.51.100.101", AutoBlockMetric.RATE_LIMITED, 2);
		assertFalse(blocks.isBlocked("198.51.100.101"));
	}

	@Test
	void reachingTheThresholdBlocksForTheConfiguredHours() {
		hit("198.51.100.102", AutoBlockMetric.RATE_LIMITED, 3);
		assertTrue(blocks.isBlocked("198.51.100.102"));
		var row = jdbc.queryForMap("select reason, blocked_by, auto, staff_id, extract(epoch from (expires_at - created_at)) as secs from ip_block where ip = '198.51.100.102'");
		assertEquals("system:auto-block", row.get("blocked_by"));
		assertEquals(1, ((Number) row.get("auto")).intValue());
		assertEquals(null, row.get("staff_id"));
		assertEquals(2 * 3600, Math.round(((Number) row.get("secs")).doubleValue()));
		assertTrue(((String) row.get("reason")).contains("2 分鐘內被限速達 3 次"));
	}

	@Test
	void theAutoBlockIsAuditedWithItsRule() {
		hit("198.51.100.103", AutoBlockMetric.LOGIN_FAILED, 4);
		var a = jdbc.queryForMap("select username, role, detail->>'auto' auto, detail->>'metric' metric, detail->>'threshold' th, detail->>'hours' hrs from staff_audit_log where action = 'IP_BLOCKED' and target_id = '198.51.100.103'");
		assertEquals("system:auto-block", a.get("username"));
		assertEquals(null, a.get("role"));
		assertEquals("1", a.get("auto"));
		assertEquals("LOGIN_FAILED", a.get("metric"));
		assertEquals("4", a.get("th"));
		assertEquals("1", a.get("hrs"));
	}

	@Test
	void eventsSpreadOutBeyondTheWindowDoNotAddUp() {
		for (int i = 0; i < 6; i++) {
			blocker.record("198.51.100.104", AutoBlockMetric.RATE_LIMITED);
			now.addAndGet(90_000);
		}
		assertFalse(blocks.isBlocked("198.51.100.104"));
	}

	@Test
	void aBurstInsideTheWindowTriggersEvenAfterSlowStart() {
		blocker.record("198.51.100.105", AutoBlockMetric.RATE_LIMITED);
		now.addAndGet(10 * 60_000);
		hit("198.51.100.105", AutoBlockMetric.RATE_LIMITED, 3);
		assertTrue(blocks.isBlocked("198.51.100.105"));
	}

	@Test
	void eachMetricAndEachIpCountsSeparately() {
		hit("198.51.100.106", AutoBlockMetric.RATE_LIMITED, 2);
		hit("198.51.100.106", AutoBlockMetric.LOGIN_FAILED, 2);
		hit("198.51.100.107", AutoBlockMetric.RATE_LIMITED, 2);
		assertFalse(blocks.isBlocked("198.51.100.106"));
		assertFalse(blocks.isBlocked("198.51.100.107"));
		hit("198.51.100.106", AutoBlockMetric.RATE_LIMITED, 1);
		assertTrue(blocks.isBlocked("198.51.100.106"));
		assertFalse(blocks.isBlocked("198.51.100.107"));
	}

	@Test
	void disabledRulesNeverBlock() {
		jdbc.update("update ip_auto_block_rule set enabled = 0 where metric = 'RATE_LIMITED'");
		em.clear();
		policy.refreshNow();
		hit("198.51.100.108", AutoBlockMetric.RATE_LIMITED, 50);
		assertFalse(blocks.isBlocked("198.51.100.108"));
		// 另一條規則不受影響
		hit("198.51.100.108", AutoBlockMetric.LOGIN_FAILED, 4);
		assertTrue(blocks.isBlocked("198.51.100.108"));
	}

	@Test
	void ruleChangesTakeEffectRightAway() {
		hit("198.51.100.109", AutoBlockMetric.RATE_LIMITED, 2);
		jdbc.update("update ip_auto_block_rule set threshold = 2 where metric = 'RATE_LIMITED'");
		em.clear();
		policy.refreshNow();
		hit("198.51.100.109", AutoBlockMetric.RATE_LIMITED, 1);
		assertTrue(blocks.isBlocked("198.51.100.109"));
	}

	@Test
	void allowlistedAddressesAreNeverAutoBlocked() {
		jdbc.update("insert into ip_allowlist (ip, note, created_by) values ('198.51.100.110', '辦公室', 'tester'), ('192.0.2.0/24', '分校', 'tester')");
		em.clear();
		policy.refreshNow();
		hit("198.51.100.110", AutoBlockMetric.RATE_LIMITED, 20);
		hit("192.0.2.77", AutoBlockMetric.RATE_LIMITED, 20);
		assertFalse(blocks.isBlocked("198.51.100.110"));
		assertFalse(blocks.isBlocked("192.0.2.77"));
		hit("192.0.3.77", AutoBlockMetric.RATE_LIMITED, 3);
		assertTrue(blocks.isBlocked("192.0.3.77"));
	}

	@Test
	void localAndProxyAddressesAreNeverAutoBlocked() {
		for (String ip : new String[] { "127.0.0.1", "::1", "169.254.1.1", "0.0.0.0" }) {
			hit(ip, AutoBlockMetric.RATE_LIMITED, 20);
			assertFalse(blocks.isBlocked(ip), ip);
		}
		assertEquals(0, audits("127.0.0.1"));
	}

	@Test
	void anAlreadyBlockedIpIsNotBlockedTwiceAndManualBlocksAreNotReplaced() {
		jdbc.update("insert into ip_block (ip, reason, blocked_by) values ('198.51.100.111', '人工永久', 'ops1')");
		em.clear();
		blocks.refreshNow();
		hit("198.51.100.111", AutoBlockMetric.RATE_LIMITED, 10);
		assertEquals("人工永久", jdbc.queryForObject("select reason from ip_block where ip = '198.51.100.111'", String.class));
		assertEquals(0, audits("198.51.100.111"));
		assertEquals(null, jdbc.queryForObject("select expires_at from ip_block where ip = '198.51.100.111'", java.time.OffsetDateTime.class));
	}

	@Test
	void theCounterResetsAfterABlockSoTheNextOneNeedsAFullRound() {
		hit("198.51.100.112", AutoBlockMetric.RATE_LIMITED, 3);
		assertTrue(blocks.isBlocked("198.51.100.112"));
		jdbc.update("delete from ip_block where ip = '198.51.100.112'");
		em.clear();
		blocks.refreshNow();
		hit("198.51.100.112", AutoBlockMetric.RATE_LIMITED, 2);
		assertFalse(blocks.isBlocked("198.51.100.112"));
		hit("198.51.100.112", AutoBlockMetric.RATE_LIMITED, 1);
		assertTrue(blocks.isBlocked("198.51.100.112"));
		assertEquals(2, audits("198.51.100.112"));
	}

	@Test
	void aBrokenBackendNeverBreaksTheRequest() {
		AutoBlocker broken = new AutoBlocker(null, blocks, resolver, now::get);
		broken.record("198.51.100.113", AutoBlockMetric.RATE_LIMITED);
	}

	@Test
	void purgeForgetsIdleCounters() {
		hit("198.51.100.114", AutoBlockMetric.RATE_LIMITED, 1);
		assertEquals(1, blocker.size());
		now.addAndGet(25 * 3_600_000L);
		blocker.purge();
		assertEquals(0, blocker.size());
	}
}
