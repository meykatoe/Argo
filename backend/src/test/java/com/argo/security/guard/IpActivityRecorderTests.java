package com.argo.security.guard;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class IpActivityRecorderTests {

	@Autowired
	IpActivityRecorder recorder;
	@Autowired
	JdbcTemplate jdbc;

	private Map<String, Object> row(String ip) {
		return jdbc.queryForMap("select * from ip_activity where ip = ?", ip);
	}

	@Test
	void flushWritesCountersAndLaterFlushesAddUp() {
		String ip = "198.51.100.40";
		recorder.rateLimited(ip);
		recorder.rateLimited(ip);
		recorder.loginFailed(ip);
		recorder.flush();
		assertEquals(2, row(ip).get("rate_limited"));
		assertEquals(1, row(ip).get("login_failed"));
		assertEquals(0, row(ip).get("blocked_hits"));

		recorder.blockedHit(ip);
		recorder.rateLimited(ip);
		recorder.flush();
		assertEquals(3, row(ip).get("rate_limited"));
		assertEquals(1, row(ip).get("login_failed"));
		assertEquals(1, row(ip).get("blocked_hits"));
	}

	@Test
	void flushWithoutEventsWritesNothing() {
		recorder.flush();
		recorder.flush();
		assertEquals(0, jdbc.queryForObject("select count(*) from ip_activity where ip = '198.51.100.41'", Integer.class));
	}

	@Test
	void eachIpHasItsOwnRow() {
		recorder.rateLimited("198.51.100.42");
		recorder.loginFailed("198.51.100.43");
		recorder.flush();
		assertEquals(1, row("198.51.100.42").get("rate_limited"));
		assertEquals(0, row("198.51.100.43").get("rate_limited"));
		assertEquals(1, row("198.51.100.43").get("login_failed"));
	}
}
