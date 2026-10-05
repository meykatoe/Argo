package com.argo.common;

import java.util.Map;
import org.springframework.http.HttpStatus;

// 帶錯誤代碼與細節的業務錯誤
public class ApiException extends RuntimeException {

	private final HttpStatus status;
	private final String code;
	private final transient Map<String, String> details;

	public ApiException(HttpStatus status, String code) {
		this(status, code, Map.of());
	}

	public ApiException(HttpStatus status, String code, Map<String, String> details) {
		super(code);
		this.status = status;
		this.code = code;
		this.details = details;
	}

	public HttpStatus getStatus() {
		return status;
	}

	public String getCode() {
		return code;
	}

	public Map<String, String> getDetails() {
		return details;
	}
}
