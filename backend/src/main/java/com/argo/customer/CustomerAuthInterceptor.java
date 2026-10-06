package com.argo.customer;

import com.argo.common.ApiException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.HandlerInterceptor;

// 需要顧客登入的端點
public class CustomerAuthInterceptor implements HandlerInterceptor {

	public static final String CUSTOMER_ATTR = "argo.customer";

	private final CustomerAuthService auth;

	public CustomerAuthInterceptor(CustomerAuthService auth) {
		this.auth = auth;
	}

	// 從標頭取出 Bearer 令牌
	public static String bearer(HttpServletRequest req) {
		String h = req.getHeader("Authorization");
		return h != null && h.startsWith("Bearer ") ? h.substring(7).trim() : null;
	}

	@Override
	public boolean preHandle(HttpServletRequest req, HttpServletResponse res, Object handler) {
		// 預檢請求放行
		if ("OPTIONS".equals(req.getMethod())) {
			return true;
		}
		CustomerAccount c = auth.authenticate(bearer(req))
				.orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED"));
		req.setAttribute(CUSTOMER_ATTR, c);
		return true;
	}
}
