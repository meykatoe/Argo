package com.argo.admin;

import com.argo.common.ApiException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

public class AdminAuthInterceptor implements HandlerInterceptor {

	public static final String STAFF_ATTR = "argo.staff";

	private final StaffAuthService auth;

	public AdminAuthInterceptor(StaffAuthService auth) {
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
		StaffAccount staff = auth.authenticate(bearer(req))
				.orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "ADMIN_UNAUTHORIZED"));
		// 未標註的端點只開放 ADMIN
		StaffRole[] allowed = { StaffRole.ADMIN };
		if (handler instanceof HandlerMethod hm) {
			RequireRole r = hm.getMethodAnnotation(RequireRole.class);
			if (r != null) {
				allowed = r.value();
			}
		}
		if (!List.of(allowed).contains(staff.getRole())) {
			throw new ApiException(HttpStatus.FORBIDDEN, "ADMIN_FORBIDDEN");
		}
		req.setAttribute(STAFF_ATTR, staff);
		return true;
	}
}
