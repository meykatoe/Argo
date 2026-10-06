package com.argo.admin;

import com.argo.common.PageResult;
import jakarta.servlet.http.HttpServletRequest;
import java.time.OffsetDateTime;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ops/audit-logs")
public class OpsAuditController {

	private final OpsAuditService service;

	public OpsAuditController(OpsAuditService service) {
		this.service = service;
	}

	// 授權由 role_menu 決定
	@GetMapping
	@RequirePermission("audit.logs")
	public PageResult<AuditLogView> list(
			@RequestParam(required = false) String username,
			@RequestParam(required = false) AuditAction action,
			@RequestParam(required = false) Boolean success,
			@RequestParam(required = false) String targetType,
			@RequestParam(required = false) String targetId,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to,
			@RequestParam(defaultValue = "1") int page,
			@RequestParam(defaultValue = "50") int size,
			HttpServletRequest req) {
		StaffAccount viewer = (StaffAccount) req.getAttribute(AdminAuthInterceptor.STAFF_ATTR);
		return service.search(viewer, new AuditLogQuery(username, action, success, targetType,
				targetId, from, to), page, size);
	}
}
