package com.argo.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

// 所有 /api 請求的第一道關卡：先擋被封鎖的 IP，再做限速
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class IpGuardFilter extends OncePerRequestFilter {

	private final ClientIpResolver resolver;
	private final IpBlockService blocks;
	private final RateLimiter limiter;
	private final IpActivityRecorder recorder;
	private final AutoBlocker autoBlocker;
	private final boolean limitEnabled;
	private final List<String> allowedOrigins;

	public IpGuardFilter(ClientIpResolver resolver, IpBlockService blocks, RateLimiter limiter,
			IpActivityRecorder recorder, AutoBlocker autoBlocker, @Value("${argo.ratelimit.enabled:true}") boolean limitEnabled,
			@Value("${argo.cors.origins:}") String origins) {
		this.resolver = resolver;
		this.blocks = blocks;
		this.limiter = limiter;
		this.recorder = recorder;
		this.autoBlocker = autoBlocker;
		this.limitEnabled = limitEnabled;
		this.allowedOrigins = Arrays.stream(origins.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
	}

	@Override
	protected boolean shouldNotFilter(HttpServletRequest req) {
		return !req.getRequestURI().startsWith("/api/");
	}

	@Override
	protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
			throws ServletException, IOException {
		String ip = resolver.resolve(req);
		if (blocks.isBlocked(ip)) {
			recorder.blockedHit(ip);
			reject(req, res, 403, "IP_BLOCKED", null);
			return;
		}
		// 預檢請求不計次
		boolean preflight = "OPTIONS".equals(req.getMethod());
		List<RateLimiter.Kind> kinds = RateLimiter.kindsFor(req.getMethod(), req.getRequestURI());
		if (limitEnabled && !preflight) {
			RateLimiter.Decision d = limiter.check(ip, kinds);
			if (!d.allowed()) {
				recorder.rateLimited(ip);
				autoBlocker.record(ip, AutoBlockMetric.RATE_LIMITED);
				res.setHeader("Retry-After", String.valueOf(d.retryAfterSeconds()));
				reject(req, res, 429, "RATE_LIMITED", d.retryAfterSeconds());
				return;
			}
		}
		chain.doFilter(req, res);
		// 登入類端點回 401 或 429 代表失敗，記在來源 IP 上
		if (kinds.contains(RateLimiter.Kind.AUTH) && (res.getStatus() == 401 || res.getStatus() == 429)) {
			recorder.loginFailed(ip);
			autoBlocker.record(ip, AutoBlockMetric.LOGIN_FAILED);
		}
	}

	// 這裡在 MVC 之前，要自己補上跨來源標頭，前端才讀得到錯誤內容
	private void reject(HttpServletRequest req, HttpServletResponse res, int status, String msg,
			Long retryAfter) throws IOException {
		String origin = req.getHeader("Origin");
		if (origin != null && allowedOrigins.contains(origin)) {
			res.setHeader("Access-Control-Allow-Origin", origin);
			res.addHeader("Vary", "Origin");
		}
		res.setStatus(status);
		res.setContentType("application/json");
		res.setCharacterEncoding(StandardCharsets.UTF_8.name());
		String data = retryAfter == null ? "null" : "{\"retryAfterSeconds\":\"" + retryAfter + "\"}";
		res.getWriter().write("{\"code\":" + status + ",\"msg\":\"" + msg + "\",\"data\":" + data + "}");
	}
}
