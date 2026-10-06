package com.argo.admin;

import com.argo.common.Result;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/card-sets")
public class AdminCardSetController {

	private final AdminCardSetService service;

	public AdminCardSetController(AdminCardSetService service) {
		this.service = service;
	}

	@GetMapping
	@RequirePermission("card.series")
	public Result<List<AdminCardSetView>> list() {
		return Result.ok(service.list());
	}

	// 上下架
	@PatchMapping("/{setId}/on-sale")
	@RequirePermission("card.series")
	public Result<AdminCardSetView> setOnSale(@PathVariable String setId,
			@Valid @RequestBody OnSaleRequest body, HttpServletRequest req) {
		return Result.ok(service.setOnSale(staff(req), setId, body.onSale()));
	}

	// 整個系列統一折扣
	@PatchMapping("/{setId}/extra-discount")
	@RequirePermission("card.series")
	public Result<AdminCardSetView> setExtraDiscount(@PathVariable String setId,
			@Valid @RequestBody ExtraDiscountRequest body, HttpServletRequest req) {
		return Result.ok(service.setExtraDiscount(staff(req), setId, body.extraDiscount()));
	}

	private static StaffAccount staff(HttpServletRequest req) {
		return (StaffAccount) req.getAttribute(AdminAuthInterceptor.STAFF_ATTR);
	}
}
