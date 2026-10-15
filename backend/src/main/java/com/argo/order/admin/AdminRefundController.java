package com.argo.order.admin;

import com.argo.common.PageResult;
import com.argo.common.Result;
import com.argo.staff.auth.AdminAuthInterceptor;
import com.argo.staff.auth.RequirePermission;
import com.argo.staff.auth.StaffAccount;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ops/refunds")
public class AdminRefundController {

	private final AdminRefundService service;

	public AdminRefundController(AdminRefundService service) {
		this.service = service;
	}

	@GetMapping
	@RequirePermission("payment.refunds")
	public Result<PageResult<AdminRefundRow>> list(@RequestParam(defaultValue = "REQUIRED") String status,
			@RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int size) {
		return Result.ok(service.list(status, page, size));
	}

	@PostMapping("/{id}/refunded")
	@RequirePermission("payment.refunds")
	public Result<AdminRefundRow> markRefunded(@PathVariable Long id, HttpServletRequest req) {
		return Result.ok(service.markRefunded((StaffAccount) req.getAttribute(AdminAuthInterceptor.STAFF_ATTR), id));
	}
}
