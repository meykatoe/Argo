package com.argo.common;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
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

	// 回傳代碼，由前端翻譯
	@ExceptionHandler(ResponseStatusException.class)
	public ResponseEntity<ErrorBody> handle(ResponseStatusException e) {
		String code = e.getReason() != null ? e.getReason() : "ERROR";
		return ResponseEntity.status(e.getStatusCode())
				.body(new ErrorBody(code, e.getStatusCode().value()));
	}

	@ExceptionHandler(ApiException.class)
	public ResponseEntity<ErrorBody> handle(ApiException e) {
		return ResponseEntity.status(e.getStatus())
				.body(new ErrorBody(e.getCode(), e.getStatus().value(), e.getDetails()));
	}

	// 欄位驗證失敗，列出有問題的欄位
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ErrorBody> handle(MethodArgumentNotValidException e) {
		Map<String, String> fields = new LinkedHashMap<>();
		e.getBindingResult().getFieldErrors()
				.forEach(f -> fields.putIfAbsent(f.getField(), String.valueOf(f.getDefaultMessage())));
		return ResponseEntity.badRequest().body(new ErrorBody("VALIDATION_ERROR", 400, fields));
	}

	// 缺少或型別不對的參數
	@ExceptionHandler({ MissingServletRequestParameterException.class,
			MethodArgumentTypeMismatchException.class })
	public ResponseEntity<ErrorBody> handleBadParam(Exception e) {
		return ResponseEntity.badRequest().body(new ErrorBody("BAD_REQUEST", 400));
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ErrorBody> handleUnreadable(HttpMessageNotReadableException e) {
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorBody("BAD_REQUEST_BODY", 400));
	}
}
