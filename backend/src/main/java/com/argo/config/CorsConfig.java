package com.argo.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig {

	@Bean
	WebMvcConfigurer corsConfigurer(@Value("${argo.cors.origins}") String[] origins) {
		return new WebMvcConfigurer() {
			@Override
			public void addCorsMappings(CorsRegistry registry) {
				// 開放讀取、送出、修改與刪除
				registry.addMapping("/api/**").allowedOrigins(origins).allowedMethods("GET", "POST", "PATCH", "DELETE");
			}
		};
	}
}
