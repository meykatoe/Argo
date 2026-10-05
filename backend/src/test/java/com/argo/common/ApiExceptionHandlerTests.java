package com.argo.common;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class ApiExceptionHandlerTests {

	private final ApiExceptionHandler handler = new ApiExceptionHandler();

	@Test
	void returnsCodeAndStatus() {
		var res = handler.handle(new ResponseStatusException(HttpStatus.NOT_FOUND, "CARD_NOT_FOUND"));
		assertEquals(404, res.getStatusCode().value());
		assertEquals(new ErrorBody("CARD_NOT_FOUND", 404), res.getBody());
	}

	@Test
	void defaultCodeWhenNoReason() {
		var res = handler.handle(new ResponseStatusException(HttpStatus.BAD_REQUEST));
		assertEquals("ERROR", res.getBody().code());
	}
}
