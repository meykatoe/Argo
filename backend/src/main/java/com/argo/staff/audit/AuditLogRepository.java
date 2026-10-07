package com.argo.staff.audit;

import java.time.OffsetDateTime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface AuditLogRepository
		extends JpaRepository<AuditLog, Long>, JpaSpecificationExecutor<AuditLog> {

	boolean existsByStaffIdAndActionAndCreatedAtAfter(Long staffId, AuditAction action, OffsetDateTime after);
}
