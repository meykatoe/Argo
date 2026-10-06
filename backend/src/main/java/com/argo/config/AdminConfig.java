package com.argo.config;

import com.argo.admin.AdminAuthInterceptor;
import com.argo.admin.StaffAuthService;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class AdminConfig implements WebMvcConfigurer {

	private final StaffAuthService auth;

	public AdminConfig(StaffAuthService auth) {
		this.auth = auth;
	}

	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		registry.addInterceptor(new AdminAuthInterceptor(auth))
				.addPathPatterns("/api/admin/**")
				.excludePathPatterns("/api/admin/auth/login");
	}
}
