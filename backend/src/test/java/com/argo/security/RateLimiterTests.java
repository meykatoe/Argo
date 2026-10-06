package com.argo.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.argo.security.RateLimiter.Decision;
import com.argo.security.RateLimiter.Kind;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

class RateLimiterTests {

	private final AtomicLong now = new AtomicLong(1_000_000);
	private final RateLimiter limiter = new RateLimiter(5, 3, 2, now::get);

	private boolean ok(String ip, Kind... kinds) {
		return limiter.check(ip, List.of(kinds)).allowed();
	}

	@Test
	void allowsUpToTheLimitThenRejects() {
		for (int i = 0; i < 5; i++) {
			assertTrue(ok("203.0.113.1", Kind.GENERAL), "request " + i);
		}
		Decision d = limiter.check("203.0.113.1", List.of(Kind.GENERAL));
		assertFalse(d.allowed());
		assertEquals(Kind.GENERAL, d.kind());
		assertTrue(d.retryAfterSeconds() >= 1 && d.retryAfterSeconds() <= 60);
	}

	@Test
	void windowResetsAfterAMinute() {
		for (int i = 0; i < 6; i++) {
			ok("203.0.113.2", Kind.GENERAL);
		}
		assertFalse(ok("203.0.113.2", Kind.GENERAL));
		now.addAndGet(59_000);
		assertFalse(ok("203.0.113.2", Kind.GENERAL));
		now.addAndGet(1_000);
		assertTrue(ok("203.0.113.2", Kind.GENERAL));
	}

	@Test
	void retryAfterShrinksAsTimePasses() {
		for (int i = 0; i < 6; i++) {
			ok("203.0.113.3", Kind.GENERAL);
		}
		assertEquals(60, limiter.check("203.0.113.3", List.of(Kind.GENERAL)).retryAfterSeconds());
		now.addAndGet(30_000);
		assertEquals(30, limiter.check("203.0.113.3", List.of(Kind.GENERAL)).retryAfterSeconds());
	}

	@Test
	void ipsAreIndependent() {
		for (int i = 0; i < 10; i++) {
			ok("203.0.113.4", Kind.GENERAL);
		}
		assertFalse(ok("203.0.113.4", Kind.GENERAL));
		assertTrue(ok("203.0.113.5", Kind.GENERAL));
	}

	@Test
	void stricterKindsTripFirstAndReportWhich() {
		assertTrue(ok("203.0.113.6", Kind.GENERAL, Kind.AUTH));
		assertTrue(ok("203.0.113.6", Kind.GENERAL, Kind.AUTH));
		assertTrue(ok("203.0.113.6", Kind.GENERAL, Kind.AUTH));
		Decision d = limiter.check("203.0.113.6", List.of(Kind.GENERAL, Kind.AUTH));
		assertFalse(d.allowed());
		assertEquals(Kind.AUTH, d.kind());
		// 一般瀏覽不受登入限制影響，且還沒到一般上限
		assertTrue(ok("203.0.113.6", Kind.GENERAL));
	}

	@Test
	void classifiesRequests() {
		assertEquals(List.of(Kind.GENERAL), RateLimiter.kindsFor("GET", "/api/cards"));
		assertEquals(List.of(Kind.GENERAL), RateLimiter.kindsFor("GET", "/api/auth/login"));
		for (String p : new String[] { "/api/auth/login", "/api/auth/register", "/api/admin/auth/login", "/api/ops/auth/login" }) {
			assertEquals(List.of(Kind.GENERAL, Kind.AUTH), RateLimiter.kindsFor("POST", p), p);
		}
		for (String p : new String[] { "/api/orders", "/api/orders/AR1-ABC/pay", "/api/orders/AR1-ABC/cancel" }) {
			assertEquals(List.of(Kind.GENERAL, Kind.CHECKOUT), RateLimiter.kindsFor("POST", p), p);
		}
		assertEquals(List.of(Kind.GENERAL), RateLimiter.kindsFor("POST", "/api/auth/logout"));
		assertEquals(List.of(Kind.GENERAL), RateLimiter.kindsFor("GET", "/api/orders/AR1-ABC"));
	}

	@Test
	void purgeDropsOldWindowsOnly() {
		ok("203.0.113.7", Kind.GENERAL);
		now.addAndGet(60_000);
		ok("203.0.113.8", Kind.GENERAL);
		limiter.purge();
		assertEquals(2, limiter.size());
		now.addAndGet(120_000);
		limiter.purge();
		assertEquals(0, limiter.size());
	}
}
