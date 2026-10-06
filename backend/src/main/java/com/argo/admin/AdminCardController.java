package com.argo.admin;

import com.argo.common.PageResult;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/cards")
public class AdminCardController {

	private final AdminCardService service;

	public AdminCardController(AdminCardService service) {
		this.service = service;
	}

	@GetMapping
	@RequireRole({ StaffRole.ADMIN, StaffRole.GENERAL })
	public PageResult<AdminCardView> list(
			@RequestParam(required = false) String keyword,
			@RequestParam(required = false) String setId,
			@RequestParam(defaultValue = "false") boolean discounted,
			@RequestParam(defaultValue = "1") int page,
			@RequestParam(defaultValue = "20") int size) {
		return service.search(keyword, setId, discounted, page, size);
	}

	// 設定額外折扣
	@PatchMapping("/{id}/extra-discount")
	@RequireRole({ StaffRole.ADMIN, StaffRole.GENERAL })
	public AdminCardView setExtraDiscount(@PathVariable Long id,
			@Valid @RequestBody ExtraDiscountRequest body, HttpServletRequest req) {
		StaffAccount staff = (StaffAccount) req.getAttribute(AdminAuthInterceptor.STAFF_ATTR);
		return service.setExtraDiscount(staff, id, body.extraDiscount());
	}
}
