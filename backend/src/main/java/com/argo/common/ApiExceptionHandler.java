package com.argo.common;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class ApiExceptionHandler {

	// code 同 HTTP 狀態，msg 為錯誤代碼，由前端翻譯
	private static ResponseEntity<Result<Map<String, String>>> fail(HttpStatusCode status, String msg,
			Map<String, String> details) {
		return ResponseEntity.status(status).body(
				Result.error(status.value(), msg, details == null || details.isEmpty() ? null : details));
	}

	@ExceptionHandler(ResponseStatusException.class)
	public ResponseEntity<Result<Map<String, String>>> handle(ResponseStatusException e) {
		return fail(e.getStatusCode(), e.getReason() != null ? e.getReason() : "ERROR", null);
	}

	@ExceptionHandler(ApiException.class)
	public ResponseEntity<Result<Map<String, String>>> handle(ApiException e) {
		return fail(e.getStatus(), e.getCode(), e.getDetails());
	}

	// 欄位驗證失敗，列出有問題的欄位
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<Result<Map<String, String>>> handle(MethodArgumentNotValidException e) {
		Map<String, String> fields = new LinkedHashMap<>();
		e.getBindingResult().getFieldErrors()
				.forEach(f -> fields.putIfAbsent(f.getField(), String.valueOf(f.getDefaultMessage())));
		return fail(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", fields);
	}

	// 缺少或型別不對的參數
	@ExceptionHandler({ MissingServletRequestParameterException.class,
			MethodArgumentTypeMismatchException.class })
	public ResponseEntity<Result<Map<String, String>>> handleBadParam(Exception e) {
		return fail(HttpStatus.BAD_REQUEST, "BAD_REQUEST", null);
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<Result<Map<String, String>>> handleUnreadable(HttpMessageNotReadableException e) {
		return fail(HttpStatus.BAD_REQUEST, "BAD_REQUEST_BODY", null);
	}
}
