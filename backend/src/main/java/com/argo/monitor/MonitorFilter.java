package com.argo.monitor;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerMapping;

// 先於 IP 關卡
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class MonitorFilter extends OncePerRequestFilter {

	private final RequestStats stats;

	public MonitorFilter(RequestStats stats) {
		this.stats = stats;
	}

	@Override
	protected boolean shouldNotFilter(HttpServletRequest req) {
		String uri = req.getRequestURI();
		// 不計自己
		return !uri.startsWith("/api/") || uri.startsWith("/api/ops/monitor")
				|| "OPTIONS".equals(req.getMethod());
	}

	@Override
	protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
			throws ServletException, IOException {
		long start = System.nanoTime();
		int status = 500;
		try {
			chain.doFilter(req, res);
			status = res.getStatus();
		}
		finally {
			stats.record(AppId.of(req.getHeader(AppId.HEADER)), req.getMethod(), path(req, status),
					status, System.nanoTime() - start);
		}
	}

	// 用路由樣式
	private static String path(HttpServletRequest req, int status) {
		Object pattern = req.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
		if (pattern != null) {
			return pattern.toString();
		}
		return status == 403 || status == 429 ? "(rejected)" : "(unmatched)";
	}
}
