package com.argo.admin;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/auth")
public class AdminAuthController {

	public record Me(String username, StaffRole role) {
	}

	private final StaffAuthService auth;

	public AdminAuthController(StaffAuthService auth) {
		this.auth = auth;
	}

	// 此路徑不經攔截器驗證
	@PostMapping("/login")
	public LoginResponse login(@Valid @RequestBody LoginRequest body) {
		return auth.login(body.username(), body.password());
	}

	@PostMapping("/logout")
	@RequireRole({ StaffRole.ADMIN, StaffRole.GENERAL, StaffRole.SERVICE })
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void logout(HttpServletRequest req) {
		auth.logout(AdminAuthInterceptor.bearer(req));
	}

	@GetMapping("/me")
	@RequireRole({ StaffRole.ADMIN, StaffRole.GENERAL, StaffRole.SERVICE })
	public Me me(HttpServletRequest req) {
		StaffAccount s = (StaffAccount) req.getAttribute(AdminAuthInterceptor.STAFF_ATTR);
		return new Me(s.getUsername(), s.getRole());
	}
}
