package com.argo.admin;

import com.argo.common.PageResult;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class OpsAuditService {

	private static final int MAX_SIZE = 100;

	private final AuditLogRepository logs;
	private final AuditLogService audit;

	public OpsAuditService(AuditLogRepository logs, AuditLogService audit) {
		this.logs = logs;
		this.audit = audit;
	}

	// 自動更新只在最近已有一筆查看紀錄時才免記，避免用它繞過稽核
	private static final Duration REFRESH_GRACE = Duration.ofMinutes(10);

	// 查看紀錄本身也會被記錄；自動更新是同一個畫面重複同樣的查詢，不再逐次記錄
	public PageResult<AuditLogView> search(StaffAccount viewer, AuditLogQuery q, int page, int size,
			boolean refresh) {
		if (page < 1 || size < 1 || size > MAX_SIZE) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "INVALID_PAGING");
		}
		if (q.from() != null && q.to() != null && !q.from().isBefore(q.to())) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "INVALID_RANGE");
		}
		var result = logs.findAll(AuditLogSpecs.of(q), PageRequest.of(page - 1, size,
				Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by(Sort.Direction.DESC, "id"))));
		if (refresh && logs.existsByStaffIdAndActionAndCreatedAtAfter(viewer.getId(),
				AuditAction.AUDIT_LOG_VIEWED, OffsetDateTime.now().minus(REFRESH_GRACE))) {
			return PageResult.of(result, AuditLogView::from);
		}
		Map<String, Object> detail = new LinkedHashMap<>();
		put(detail, "username", q.username());
		put(detail, "action", q.action());
		put(detail, "success", q.success());
		put(detail, "targetType", q.targetType());
		put(detail, "targetId", q.targetId());
		put(detail, "from", q.from());
		put(detail, "to", q.to());
		detail.put("page", page);
		audit.record(viewer, null, AuditAction.AUDIT_LOG_VIEWED, true, null, null, detail);
		return PageResult.of(result, AuditLogView::from);
	}

	private static void put(Map<String, Object> m, String key, Object value) {
		if (value != null && !value.toString().isBlank()) {
			m.put(key, value.toString());
		}
	}
}
