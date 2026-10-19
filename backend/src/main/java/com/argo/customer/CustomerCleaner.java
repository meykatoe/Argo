package com.argo.customer;

import java.time.OffsetDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

// 每日清掉過期登入、待驗證註冊與重設密碼連結
@Component
public class CustomerCleaner {

	private static final Logger log = LoggerFactory.getLogger(CustomerCleaner.class);

	private final CustomerSessionRepository sessions;
	private final PendingRegistrationRepository pending;
	private final CustomerEmailTokenRepository tokens;

	public CustomerCleaner(CustomerSessionRepository sessions, PendingRegistrationRepository pending,
			CustomerEmailTokenRepository tokens) {
		this.sessions = sessions;
		this.pending = pending;
		this.tokens = tokens;
	}

	@Scheduled(cron = "0 45 3 * * *")
	public void scheduled() {
		try {
			purge();
		} catch (Exception e) {
			log.warn("customer purge failed", e);
		}
	}

	@Transactional
	public int purge() {
		OffsetDateTime now = OffsetDateTime.now();
		int n = sessions.deleteExpired(now) + pending.deleteExpired(now) + tokens.deleteExpired(now);
		if (n > 0) {
			log.info("已清除過期顧客資料 {} 筆", n);
		}
		return n;
	}
}
