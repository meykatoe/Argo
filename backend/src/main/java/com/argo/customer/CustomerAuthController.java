package com.argo.customer;

import com.argo.common.Result;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class CustomerAuthController {

	private final CustomerAuthService auth;

	public CustomerAuthController(CustomerAuthService auth) {
		this.auth = auth;
	}

	// 註冊與登入不經攔截器
	@PostMapping("/register")
	public Result<CustomerAuthView> register(@Valid @RequestBody RegisterRequest body) {
		return Result.ok(auth.register(body.email(), body.password(), body.name()));
	}

	@PostMapping("/login")
	public Result<CustomerAuthView> login(@Valid @RequestBody CustomerLoginRequest body) {
		return Result.ok(auth.login(body.email(), body.password()));
	}

	@PostMapping("/logout")
	public Result<Void> logout(HttpServletRequest req) {
		auth.logout(CustomerAuthInterceptor.bearer(req));
		return Result.ok();
	}

	@GetMapping("/me")
	public Result<CustomerMe> me(HttpServletRequest req) {
		return Result.ok(CustomerMe.from((CustomerAccount) req.getAttribute(CustomerAuthInterceptor.CUSTOMER_ATTR)));
	}
}
