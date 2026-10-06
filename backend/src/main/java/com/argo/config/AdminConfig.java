package com.argo.config;

import com.argo.admin.AdminAuthInterceptor;
import com.argo.admin.AuditLogService;
import com.argo.admin.StaffAuthService;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class AdminConfig implements WebMvcConfigurer {

	private final StaffAuthService auth;
	private final AuditLogService audit;

	public AdminConfig(StaffAuthService auth, AuditLogService audit) {
		this.auth = auth;
		this.audit = audit;
	}

	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		registry.addInterceptor(new AdminAuthInterceptor(auth, audit))
				.addPathPatterns("/api/admin/**", "/api/ops/**")
				.excludePathPatterns("/api/admin/auth/login", "/api/ops/auth/login");
	}
}
