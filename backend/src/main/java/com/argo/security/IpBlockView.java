package com.argo.security;

import java.time.OffsetDateTime;

public record IpBlockView(String ip, String reason, String blockedBy, OffsetDateTime createdAt,
		OffsetDateTime expiresAt) {

	public static IpBlockView from(IpBlock b) {
		return new IpBlockView(b.getIp(), b.getReason(), b.getBlockedBy(), b.getCreatedAt(), b.getExpiresAt());
	}
}
