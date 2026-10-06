package com.argo.config;

import com.argo.admin.AdminAuthInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class AdminConfig implements WebMvcConfigurer {

	private final String token;

	public AdminConfig(@Value("${argo.admin.token:}") String token) {
		this.token = token;
	}

	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		registry.addInterceptor(new AdminAuthInterceptor(token)).addPathPatterns("/api/admin/**");
	}
}
