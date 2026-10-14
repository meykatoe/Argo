package com.argo.order;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class ExchangeRateServiceTests {

	private static final BigDecimal FALLBACK = new BigDecimal("30");

	private static class MutableClock extends Clock {
		Instant now = Instant.parse("2026-01-01T00:00:00Z");

		@Override
		public java.time.ZoneId getZone() {
			return ZoneOffset.UTC;
		}

		@Override
		public Clock withZone(java.time.ZoneId zone) {
			return this;
		}

		@Override
		public Instant instant() {
			return now;
		}
	}

	@Test
	void roundsToWholeTwd() {
		ExchangeRateService fx = new ExchangeRateService(() -> new BigDecimal("32.141562"),
				new MutableClock(), Duration.ofMinutes(60), FALLBACK);
		ExchangeRateService.Quote q = fx.toTwd(new BigDecimal("10.00"));
		assertThat(q.twd()).isEqualTo(321);
		assertThat(q.rate()).isEqualByComparingTo("32.141562");
	}

	@Test
	void cachesWithinTtl() {
		AtomicInteger calls = new AtomicInteger();
		MutableClock clock = new MutableClock();
		ExchangeRateService fx = new ExchangeRateService(() -> {
			calls.incrementAndGet();
			return new BigDecimal("32");
		}, clock, Duration.ofMinutes(60), FALLBACK);
		fx.rate();
		clock.now = clock.now.plus(Duration.ofMinutes(59));
		fx.rate();
		assertThat(calls.get()).isEqualTo(1);
		clock.now = clock.now.plus(Duration.ofMinutes(2));
		fx.rate();
		assertThat(calls.get()).isEqualTo(2);
	}

	@Test
	void usesFallbackWhenNeverFetched() {
		ExchangeRateService fx = new ExchangeRateService(() -> {
			throw new IllegalStateException("down");
		}, new MutableClock(), Duration.ofMinutes(60), FALLBACK);
		assertThat(fx.rate()).isEqualByComparingTo(FALLBACK);
	}

	@Test
	void keepsLastRateWhenRefreshFails() {
		MutableClock clock = new MutableClock();
		boolean[] down = { false };
		ExchangeRateService fx = new ExchangeRateService(() -> {
			if (down[0]) {
				throw new IllegalStateException("down");
			}
			return new BigDecimal("31");
		}, clock, Duration.ofMinutes(60), FALLBACK);
		fx.rate();
		down[0] = true;
		clock.now = clock.now.plus(Duration.ofMinutes(61));
		assertThat(fx.rate()).isEqualByComparingTo("31");
	}

	@Test
	void rejectsAbsurdRate() {
		ExchangeRateService fx = new ExchangeRateService(() -> BigDecimal.ZERO, new MutableClock(),
				Duration.ofMinutes(60), FALLBACK);
		assertThat(fx.rate()).isEqualByComparingTo(FALLBACK);
	}
}
