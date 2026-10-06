package com.argo.config;

import com.argo.customer.CustomerAuthInterceptor;
import com.argo.customer.CustomerAuthService;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CustomerConfig implements WebMvcConfigurer {

	private final CustomerAuthService auth;

	public CustomerConfig(CustomerAuthService auth) {
		this.auth = auth;
	}

	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		registry.addInterceptor(new CustomerAuthInterceptor(auth))
				.addPathPatterns("/api/me/**", "/api/auth/logout", "/api/auth/me");
	}
}
