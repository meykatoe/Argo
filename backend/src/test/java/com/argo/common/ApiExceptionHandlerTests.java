package com.argo.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class ApiExceptionHandlerTests {

	private final ApiExceptionHandler handler = new ApiExceptionHandler();

	@Test
	void frameworkErrorKeepsHttpStatusAsCode() {
		var res = handler.handle(new ResponseStatusException(HttpStatus.NOT_FOUND, "CARD_NOT_FOUND"));
		assertEquals(404, res.getStatusCode().value());
		assertEquals(404, res.getBody().code());
		assertEquals("CARD_NOT_FOUND", res.getBody().msg());
		assertNull(res.getBody().data());
	}

	@Test
	void codeIsErrorNumberAndHttpStatusStaysReal() {
		for (ErrorCode c : ErrorCode.values()) {
			var res = handler.handle(new ApiException(c));
			assertEquals(c.status().value(), res.getStatusCode().value(), c.name());
			assertEquals(c.number(), res.getBody().code(), c.name());
			assertEquals(c.name(), res.getBody().msg(), c.name());
		}
	}

	@Test
	void defaultMsgWhenNoReason() {
		var res = handler.handle(new ResponseStatusException(HttpStatus.BAD_REQUEST));
		assertEquals("ERROR", res.getBody().msg());
	}

	@Test
	void detailsGoIntoData() {
		var res = handler.handle(new ApiException(ErrorCode.ITEM_UNAVAILABLE, Map.of("cardId", "7")));
		assertEquals(409, res.getStatusCode().value());
		assertEquals(4003, res.getBody().code());
		assertEquals(Map.of("cardId", "7"), res.getBody().data());
	}

	@Test
	void successResultUsesCode200() {
		var r = Result.ok("x");
		assertEquals(200, r.code());
		assertEquals("OK", r.msg());
		assertEquals("x", r.data());
		assertNull(Result.ok().data());
	}
}
