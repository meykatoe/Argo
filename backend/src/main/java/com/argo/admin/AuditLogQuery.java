package com.argo.admin;

import java.time.OffsetDateTime;

public record AuditLogQuery(String username, AuditAction action, Boolean success,
		String targetType, String targetId, OffsetDateTime from, OffsetDateTime to) {
}
