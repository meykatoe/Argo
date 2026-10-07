package com.argo.security.block;

import java.time.OffsetDateTime;

// blocked 為 1 代表目前被封鎖
public record IpActivityView(String ip, long rateLimited, long loginFailed, long blockedHits,
		OffsetDateTime firstSeen, OffsetDateTime lastSeen, int blocked, OffsetDateTime blockExpiresAt,
		String blockReason, int blockAuto) {
}
