package com.argo.staff.auth;

import com.argo.staff.audit.AuditLogService;
import com.argo.staff.auth.AdminAuthInterceptor;
import com.argo.staff.auth.StaffAuthService;
import com.argo.staff.menu.MenuService;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class AdminConfig implements WebMvcConfigurer {

	private final StaffAuthService auth;
	private final AuditLogService audit;
	private final MenuService menus;

	public AdminConfig(StaffAuthService auth, AuditLogService audit, MenuService menus) {
		this.menus = menus;
		this.auth = auth;
		this.audit = audit;
	}

	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		registry.addInterceptor(new AdminAuthInterceptor(auth, audit, menus))
				.addPathPatterns("/api/admin/**", "/api/ops/**")
				.excludePathPatterns("/api/admin/auth/login", "/api/ops/auth/login");
	}
}
