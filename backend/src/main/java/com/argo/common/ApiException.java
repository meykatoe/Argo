package com.argo.common;

import java.util.Map;
import org.springframework.http.HttpStatus;

// 帶錯誤碼與細節的業務錯誤
public class ApiException extends RuntimeException {

	private final ErrorCode error;
	private final transient Map<String, String> details;

	public ApiException(ErrorCode error) {
		this(error, Map.of());
	}

	public ApiException(ErrorCode error, Map<String, String> details) {
		super(error.name());
		this.error = error;
		this.details = details;
	}

	public ErrorCode getError() {
		return error;
	}

	public HttpStatus getStatus() {
		return error.status();
	}

	public String getCode() {
		return error.name();
	}

	public Map<String, String> getDetails() {
		return details;
	}
}
