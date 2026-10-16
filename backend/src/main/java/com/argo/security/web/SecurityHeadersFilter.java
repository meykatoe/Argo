package com.argo.security.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

// 加安全標頭
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 5)
public class SecurityHeadersFilter extends OncePerRequestFilter {

	@Override
	protected boolean shouldNotFilter(HttpServletRequest req) {
		return !req.getRequestURI().startsWith("/api/");
	}

	@Override
	protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
			throws ServletException, IOException {
		res.setHeader("X-Content-Type-Options", "nosniff");
		res.setHeader("X-Frame-Options", "DENY");
		res.setHeader("Referrer-Policy", "no-referrer");
		res.setHeader("Content-Security-Policy", "default-src 'none'; frame-ancestors 'none'");
		res.setHeader("Cache-Control", "no-store");
		if (req.isSecure() || "https".equalsIgnoreCase(req.getHeader("X-Forwarded-Proto"))) {
			res.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains");
		}
		chain.doFilter(req, res);
	}
}
