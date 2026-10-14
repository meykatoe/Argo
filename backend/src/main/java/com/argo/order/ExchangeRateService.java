package com.argo.order;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ExchangeRateService {

	private static final Logger log = LoggerFactory.getLogger(ExchangeRateService.class);
	private static final Duration RETRY = Duration.ofMinutes(1);
	private static final BigDecimal MAX_RATE = BigDecimal.valueOf(1000);

	// 換算結果：匯率與整數新台幣
	public record Quote(BigDecimal rate, long twd) {
	}

	private final RateSource source;
	private final Clock clock;
	private final Duration ttl;
	private final BigDecimal fallback;

	private BigDecimal cached;
	private Instant fetchedAt;
	private Instant retryAt;

	@Autowired
	public ExchangeRateService(RateSource source, @Value("${argo.fx.ttl-minutes:60}") long ttlMinutes,
			@Value("${argo.fx.fallback-rate:32.0}") BigDecimal fallback) {
		this(source, Clock.systemUTC(), Duration.ofMinutes(ttlMinutes), fallback);
	}

	ExchangeRateService(RateSource source, Clock clock, Duration ttl, BigDecimal fallback) {
		this.source = source;
		this.clock = clock;
		this.ttl = ttl;
		this.fallback = fallback;
	}

	public Quote toTwd(BigDecimal usd) {
		BigDecimal rate = rate();
		return new Quote(rate, usd.multiply(rate).setScale(0, RoundingMode.HALF_UP).longValueExact());
	}

	// 過期才重抓，失敗沿用舊值，沒有舊值用備援
	public synchronized BigDecimal rate() {
		Instant now = clock.instant();
		if (cached != null && now.isBefore(fetchedAt.plus(ttl))) {
			return cached;
		}
		// 從未成功過時，失敗後一分鐘內不重試
		if (cached == null && retryAt != null && now.isBefore(retryAt)) {
			return fallback;
		}
		try {
			BigDecimal fresh = source.usdToTwd();
			if (fresh.signum() <= 0 || fresh.compareTo(MAX_RATE) > 0) {
				throw new IllegalStateException("fx rate out of range");
			}
			cached = fresh;
			fetchedAt = now;
		} catch (RuntimeException e) {
			log.warn("fx fetch failed, using {}: {}", cached != null ? "last rate" : "fallback", e.toString());
			if (cached == null) {
				retryAt = now.plus(RETRY);
				return fallback;
			}
			fetchedAt = now;
		}
		return cached;
	}
}
