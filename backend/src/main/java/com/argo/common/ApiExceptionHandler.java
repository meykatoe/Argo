package com.argo.common;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class ApiExceptionHandler {

	// 回傳代碼，由前端翻譯
	@ExceptionHandler(ResponseStatusException.class)
	public ResponseEntity<ErrorBody> handle(ResponseStatusException e) {
		String code = e.getReason() != null ? e.getReason() : "ERROR";
		return ResponseEntity.status(e.getStatusCode())
				.body(new ErrorBody(code, e.getStatusCode().value()));
	}
}
