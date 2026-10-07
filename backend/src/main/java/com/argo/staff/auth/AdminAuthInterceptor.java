package com.argo.staff.auth;

import com.argo.common.ApiException;
import com.argo.common.ErrorCode;
import com.argo.staff.audit.AuditAction;
import com.argo.staff.audit.AuditLogService;
import com.argo.staff.menu.MenuService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

public class AdminAuthInterceptor implements HandlerInterceptor {

	public static final String STAFF_ATTR = "argo.staff";

	private final StaffAuthService auth;
	private final AuditLogService audit;
	private final MenuService menus;

	public AdminAuthInterceptor(StaffAuthService auth, AuditLogService audit, MenuService menus) {
		this.menus = menus;
		this.auth = auth;
		this.audit = audit;
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
				.orElseThrow(() -> new ApiException(ErrorCode.ADMIN_UNAUTHORIZED));
		// 未標註的端點一律拒絕
		boolean ok = false;
		String need = "(none)";
		if (handler instanceof HandlerMethod hm) {
			RequirePermission p = hm.getMethodAnnotation(RequirePermission.class);
			if (p != null) {
				need = p.value();
				ok = menus.hasPermission(staff.getRole(), need);
			} else if (hm.hasMethodAnnotation(AnyStaff.class)) {
				ok = true;
			}
		}
		if (!ok) {
			audit.record(staff, null, AuditAction.ACCESS_DENIED, false, "ENDPOINT", req.getRequestURI(),
					Map.of("method", req.getMethod(), "permission", need));
			throw new ApiException(ErrorCode.ADMIN_FORBIDDEN);
		}
		req.setAttribute(STAFF_ATTR, staff);
		return true;
	}
}
