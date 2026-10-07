package com.argo.staff.audit;

import java.time.OffsetDateTime;

public record AuditLogQuery(String username, AuditAction action, Boolean success,
		String targetType, String targetId, OffsetDateTime from, OffsetDateTime to) {
}
