package com.argo.monitor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.argo.monitor.RequestStats.Agg;
import com.argo.monitor.RequestStats.Key;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import org.junit.jupiter.api.Test;

class RequestStatsTests {

	static final class TestClock extends Clock {

		long millis = 1_000_000L * 60_000;

		@Override
		public ZoneOffset getZone() {
			return ZoneOffset.UTC;
		}

		@Override
		public Clock withZone(java.time.ZoneId zone) {
			return this;
		}

		@Override
		public Instant instant() {
			return Instant.ofEpochMilli(millis);
		}
	}

	static final long MS = 1_000_000L;

	final TestClock clock = new TestClock();
	final RequestStats stats = new RequestStats(clock);
	final Key admin = new Key("admin", "GET", "/api/x");

	@Test
	void countsByAppMethodAndPath() {
		stats.record("admin", "GET", "/api/x", 200, 10 * MS);
		stats.record("admin", "GET", "/api/x", 200, 10 * MS);
		stats.record("ops", "GET", "/api/x", 200, 10 * MS);
		stats.record("admin", "POST", "/api/x", 200, 10 * MS);
		Map<Key, Agg> snap = stats.snapshot(5);
		assertEquals(2, snap.get(admin).count);
		assertEquals(1, snap.get(new Key("ops", "GET", "/api/x")).count);
		assertEquals(1, snap.get(new Key("admin", "POST", "/api/x")).count);
	}

	@Test
	void separatesServerAndClientErrors() {
		stats.record("admin", "GET", "/api/x", 500, MS);
		stats.record("admin", "GET", "/api/x", 503, MS);
		stats.record("admin", "GET", "/api/x", 404, MS);
		stats.record("admin", "GET", "/api/x", 200, MS);
		Agg a = stats.snapshot(5).get(admin);
		assertEquals(2, a.errors5xx);
		assertEquals(1, a.errors4xx);
		assertEquals(50.0, a.errorRate(), 0.001);
	}

	@Test
	void oldMinutesFallOutOfTheWindow() {
		stats.record("admin", "GET", "/api/x", 200, MS);
		clock.millis += 3 * 60_000;
		stats.record("admin", "GET", "/api/x", 200, MS);
		assertEquals(2, stats.snapshot(5).get(admin).count);
		assertEquals(1, stats.snapshot(3).get(admin).count);
		assertEquals(1, stats.snapshot(1).get(admin).count);
		clock.millis += 10 * 60_000;
		assertNull(stats.snapshot(5).get(admin));
	}

	@Test
	void reusedSlotAfterAnHourStartsClean() {
		stats.record("admin", "GET", "/api/x", 200, MS);
		clock.millis += 60 * 60_000;
		stats.record("admin", "GET", "/api/x", 200, MS);
		assertEquals(1, stats.snapshot(60).get(admin).count);
	}

	@Test
	void percentileIsABoundedUpperEstimate() {
		for (int i = 0; i < 95; i++) {
			stats.record("admin", "GET", "/api/x", 200, 20 * MS);
		}
		for (int i = 0; i < 5; i++) {
			stats.record("admin", "GET", "/api/x", 200, 3000 * MS);
		}
		Agg a = stats.snapshot(5).get(admin);
		assertEquals(25.0, a.percentileMs(0.95), 0.001);
		assertEquals(3000.0, a.percentileMs(0.99), 0.001);
		assertEquals(3000.0, a.maxMs(), 0.001);
		assertTrue(a.avgMs() > 20 && a.avgMs() < 400);
	}

	@Test
	void appNamesAreWhitelisted() {
		assertEquals("admin", AppId.of(" admin "));
		assertEquals("official-site", AppId.of("official-site"));
		assertEquals("unknown", AppId.of("evil-app"));
		assertEquals("unknown", AppId.of(null));
		assertEquals("unknown", AppId.of(""));
	}
}
