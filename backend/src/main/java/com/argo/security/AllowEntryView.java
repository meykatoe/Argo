package com.argo.security;

import java.time.OffsetDateTime;

public record AllowEntryView(String ip, String note, String createdBy, OffsetDateTime createdAt) {

	public static AllowEntryView from(IpAllowEntry e) {
		return new AllowEntryView(e.getIp(), e.getNote(), e.getCreatedBy(), e.getCreatedAt());
	}
}
