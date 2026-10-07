package com.argo.staff.audit;

import com.argo.common.Flag;
import com.argo.staff.auth.StaffRole;
import java.time.OffsetDateTime;
import java.util.Map;

public record AuditLogView(Long id, Long staffId, String username, StaffRole role,
		AuditAction action, String targetType, String targetId, Map<String, Object> detail,
		int success, String ip, String userAgent, OffsetDateTime createdAt) {

	public static AuditLogView from(AuditLog l) {
		return new AuditLogView(l.getId(), l.getStaffId(), l.getUsername(), l.getRole(),
				l.getAction(), l.getTargetType(), l.getTargetId(), l.getDetail(), Flag.of(l.isSuccess()),
				l.getIp(), l.getUserAgent(), l.getCreatedAt());
	}
}
