package com.argo.order.admin;

import com.argo.common.PageResult;
import com.argo.common.Result;
import com.argo.order.OrderStatus;
import com.argo.staff.auth.RequirePermission;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
}
