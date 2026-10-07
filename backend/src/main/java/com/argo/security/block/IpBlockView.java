package com.argo.security.block;

import com.argo.common.Flag;
import java.time.OffsetDateTime;

// auto 為 1 代表系統自動封鎖
public record IpBlockView(String ip, String reason, String blockedBy, OffsetDateTime createdAt,
		OffsetDateTime expiresAt, int auto) {

	public static IpBlockView from(IpBlock b) {
		return new IpBlockView(b.getIp(), b.getReason(), b.getBlockedBy(), b.getCreatedAt(), b.getExpiresAt(),
				Flag.of(b.isAuto()));
	}
}
