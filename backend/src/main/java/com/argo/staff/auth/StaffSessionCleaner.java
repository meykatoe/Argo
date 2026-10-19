package com.argo.staff.auth;

import java.time.OffsetDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

// 每日清掉過期登入，稽核紀錄不碰
@Component
public class StaffSessionCleaner {

	private static final Logger log = LoggerFactory.getLogger(StaffSessionCleaner.class);

	private final StaffSessionRepository sessions;

	public StaffSessionCleaner(StaffSessionRepository sessions) {
		this.sessions = sessions;
	}

	@Scheduled(cron = "0 40 3 * * *")
	public void scheduled() {
		try {
			purge();
		} catch (Exception e) {
			log.warn("staff session purge failed", e);
		}
	}

	@Transactional
	public int purge() {
		int n = sessions.deleteExpired(OffsetDateTime.now());
		if (n > 0) {
			log.info("已清除過期員工登入 {} 筆", n);
		}
		return n;
	}
}
