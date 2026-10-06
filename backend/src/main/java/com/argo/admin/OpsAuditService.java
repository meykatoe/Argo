package com.argo.admin;

import com.argo.common.PageResult;
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

	// 查看紀錄本身也會被記錄
	public PageResult<AuditLogView> search(StaffAccount viewer, AuditLogQuery q, int page, int size) {
		if (page < 1 || size < 1 || size > MAX_SIZE) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "INVALID_PAGING");
		}
		if (q.from() != null && q.to() != null && !q.from().isBefore(q.to())) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "INVALID_RANGE");
		}
		var result = logs.findAll(AuditLogSpecs.of(q), PageRequest.of(page - 1, size,
				Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by(Sort.Direction.DESC, "id"))));
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
