package com.argo.customer;

import com.argo.common.Flag;
import com.argo.common.Result;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class CustomerAuthController {

	private final CustomerAuthService auth;
	private final CustomerEmailService emails;

	public CustomerAuthController(CustomerAuthService auth, CustomerEmailService emails) {
		this.auth = auth;
		this.emails = emails;
	}

	// 註冊與登入不經攔截器
	@PostMapping("/register")
	public Result<Void> register(@Valid @RequestBody RegisterRequest body) {
		auth.register(body.username(), body.email(), body.password(), body.name());
		return Result.ok();
	}

	@PostMapping("/login")
	public Result<CustomerAuthView> login(@Valid @RequestBody CustomerLoginRequest body) {
		return Result.ok(auth.login(body.account(), body.password()));
	}

	// 帳號是否可用，1 可用、0 已被使用
	@GetMapping("/username-available")
	public Result<UsernameCheck> usernameAvailable(@RequestParam String username) {
		return Result.ok(new UsernameCheck(Flag.of(auth.usernameAvailable(username))));
	}

	// 點信中連結驗證，通過才建立帳號並登入
	@PostMapping("/verify-email")
	public Result<CustomerAuthView> verifyEmail(@Valid @RequestBody TokenRequest body) {
		return Result.ok(auth.completeRegistration(body.token()));
	}

	// 重寄驗證信，沒有待驗證資料也回成功
	@PostMapping("/resend-verification")
	public Result<Void> resendVerification(@Valid @RequestBody EmailRequest body) {
		auth.resendRegistration(body.email());
		return Result.ok();
	}

	// 一律回成功，不透露信箱是否註冊過
	@PostMapping("/forgot-password")
	public Result<Void> forgotPassword(@Valid @RequestBody EmailRequest body) {
		emails.sendReset(body.email());
		return Result.ok();
	}

	@PostMapping("/reset-password")
	public Result<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest body) {
		auth.resetPassword(body.token(), body.password());
		return Result.ok();
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
