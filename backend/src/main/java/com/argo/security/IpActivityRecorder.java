package com.argo.security;

import jakarta.annotation.PreDestroy;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

// 異常事件先在記憶體累計，定時批次寫入，避免被攻擊時每個請求都寫資料庫
@Component
public class IpActivityRecorder {

	private static final Logger log = LoggerFactory.getLogger(IpActivityRecorder.class);

	private static final class Counters {
		final AtomicLong rateLimited = new AtomicLong();
		final AtomicLong loginFailed = new AtomicLong();
		final AtomicLong blockedHits = new AtomicLong();
		volatile Instant first = Instant.now();
		volatile Instant last = Instant.now();
	}

	private final ConcurrentHashMap<String, Counters> counters = new ConcurrentHashMap<>();
	private final JdbcTemplate jdbc;

	public IpActivityRecorder(JdbcTemplate jdbc) {
		this.jdbc = jdbc;
	}

	public void rateLimited(String ip) {
		touch(ip).rateLimited.incrementAndGet();
	}

	public void loginFailed(String ip) {
		touch(ip).loginFailed.incrementAndGet();
	}

	public void blockedHit(String ip) {
		touch(ip).blockedHits.incrementAndGet();
	}

	private Counters touch(String ip) {
		Counters c = counters.computeIfAbsent(ip, k -> new Counters());
		c.last = Instant.now();
		return c;
	}

	// 每 10 秒取走累計值寫入資料庫，同一天同一個 IP 的次數會加總
	@Scheduled(fixedDelayString = "${argo.security.flush-millis:10000}", initialDelayString = "${argo.security.flush-millis:10000}")
	public void flushScheduled() {
		flush();
	}

	// 關閉前也要寫入
	@PreDestroy
	public void flush() {
		LocalDate day = LocalDate.now();
		counters.forEach((ip, c) -> {
			long rl = c.rateLimited.getAndSet(0);
			long lf = c.loginFailed.getAndSet(0);
			long bh = c.blockedHits.getAndSet(0);
			if (rl + lf + bh == 0) {
				counters.remove(ip, c);
				return;
			}
			try {
				jdbc.update("""
						insert into ip_activity (ip, day, rate_limited, login_failed, blocked_hits, first_seen, last_seen)
						values (?, ?, ?, ?, ?, ?, ?)
						on conflict (ip, day) do update set
						  rate_limited = ip_activity.rate_limited + excluded.rate_limited,
						  login_failed = ip_activity.login_failed + excluded.login_failed,
						  blocked_hits = ip_activity.blocked_hits + excluded.blocked_hits,
						  last_seen = excluded.last_seen
						""", ip, java.sql.Date.valueOf(day), rl, lf, bh, Timestamp.from(c.first), Timestamp.from(c.last));
				c.first = c.last;
			} catch (RuntimeException e) {
				// 寫入失敗就還回去，下次再寫
				c.rateLimited.addAndGet(rl);
				c.loginFailed.addAndGet(lf);
				c.blockedHits.addAndGet(bh);
				log.warn("寫入 IP 異常紀錄失敗：{}", e.getMessage());
			}
		});
	}

	// 清掉太舊的資料
	@Scheduled(cron = "0 30 3 * * *")
	public void purgeOld() {
		jdbc.update("delete from ip_activity where day < ?", java.sql.Date.valueOf(LocalDate.now().minusDays(30)));
	}
}
