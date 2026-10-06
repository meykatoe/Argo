package com.argo.admin;

import com.argo.common.ApiException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.HandlerInterceptor;

public class AdminAuthInterceptor implements HandlerInterceptor {

	public static final String HEADER = "X-Admin-Token";

	private final byte[] token;

	public AdminAuthInterceptor(String token) {
		this.token = token.getBytes(StandardCharsets.UTF_8);
	}

	@Override
	public boolean preHandle(HttpServletRequest req, HttpServletResponse res, Object handler) {
		// 預檢請求放行
		if ("OPTIONS".equals(req.getMethod())) {
			return true;
		}
		String given = req.getHeader(HEADER);
		// 未設定令牌則一律拒絕
		if (token.length == 0 || given == null
				|| !MessageDigest.isEqual(token, given.getBytes(StandardCharsets.UTF_8))) {
			throw new ApiException(HttpStatus.UNAUTHORIZED, "ADMIN_UNAUTHORIZED");
		}
		return true;
	}
}
