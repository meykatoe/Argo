package com.argo.order.admin;

import com.argo.common.PageResult;
import com.argo.common.Result;
import com.argo.order.OrderStatus;
import com.argo.staff.auth.AdminAuthInterceptor;
import com.argo.staff.auth.RequirePermission;
import com.argo.staff.auth.StaffAccount;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/orders")
public class AdminOrderController {

	private final AdminOrderService service;

	public AdminOrderController(AdminOrderService service) {
		this.service = service;
	}

	@GetMapping
	@RequirePermission("order.list")
	public Result<PageResult<AdminOrderRow>> list(
			@RequestParam(required = false) String keyword,
			@RequestParam(required = false) OrderStatus status,
			@RequestParam(defaultValue = "1") int page,
			@RequestParam(defaultValue = "20") int size) {
		return Result.ok(service.search(keyword, status, page, size));
	}

	@GetMapping("/{orderNo}")
	@RequirePermission("order.list")
	public Result<AdminOrderView> get(@PathVariable String orderNo) {
		return Result.ok(service.get(orderNo));
	}

	@PostMapping("/{orderNo}/ship")
	@RequirePermission("order.manage")
	public Result<AdminOrderView> ship(@PathVariable String orderNo,
			@Valid @RequestBody ShipRequest body, HttpServletRequest req) {
		return Result.ok(service.ship(staff(req), orderNo, body.trackingNo()));
	}

	@PostMapping("/{orderNo}/complete")
	@RequirePermission("order.manage")
	public Result<AdminOrderView> complete(@PathVariable String orderNo, HttpServletRequest req) {
		return Result.ok(service.complete(staff(req), orderNo));
	}

	@PostMapping("/{orderNo}/cancel")
	@RequirePermission("order.manage")
	public Result<AdminOrderView> cancel(@PathVariable String orderNo, HttpServletRequest req) {
		return Result.ok(service.cancel(staff(req), orderNo));
	}

	@PatchMapping("/{orderNo}/note")
	@RequirePermission("order.manage")
	public Result<AdminOrderView> setNote(@PathVariable String orderNo,
			@Valid @RequestBody NoteRequest body, HttpServletRequest req) {
		return Result.ok(service.setNote(staff(req), orderNo, body.note()));
	}

	private static StaffAccount staff(HttpServletRequest req) {
		return (StaffAccount) req.getAttribute(AdminAuthInterceptor.STAFF_ATTR);
	}
}
