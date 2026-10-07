package com.argo.staff.auth;

import com.argo.common.Result;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

// 業務後台與運維後台各有登入入口，其餘共用
@RestController
public class AdminAuthController {

	public record Me(String username, StaffRole role) {
	}

	private final StaffAuthService auth;

	public AdminAuthController(StaffAuthService auth) {
		this.auth = auth;
	}

	// 登入路徑不經攔截器驗證
	@PostMapping("/api/admin/auth/login")
	public Result<LoginResponse> adminLogin(@Valid @RequestBody LoginRequest body) {
		return Result.ok(auth.login(body.username(), body.password(), LoginPortal.ADMIN));
	}

	@PostMapping("/api/ops/auth/login")
	public Result<LoginResponse> opsLogin(@Valid @RequestBody LoginRequest body) {
		return Result.ok(auth.login(body.username(), body.password(), LoginPortal.OPS));
	}

	@PostMapping({ "/api/admin/auth/logout", "/api/ops/auth/logout" })
	@AnyStaff
	public Result<Void> logout(HttpServletRequest req) {
		auth.logout(AdminAuthInterceptor.bearer(req));
		return Result.ok();
	}

	@GetMapping({ "/api/admin/auth/me", "/api/ops/auth/me" })
	@AnyStaff
	public Result<Me> me(HttpServletRequest req) {
		StaffAccount s = (StaffAccount) req.getAttribute(AdminAuthInterceptor.STAFF_ATTR);
		return Result.ok(new Me(s.getUsername(), s.getRole()));
	}
}
