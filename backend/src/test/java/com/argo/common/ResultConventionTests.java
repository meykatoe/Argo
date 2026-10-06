package com.argo.common;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

// 專案規則：所有對前端的端點一律回傳 Result<T>
@SpringBootTest
class ResultConventionTests {

	@Autowired
	@Qualifier("requestMappingHandlerMapping")
	RequestMappingHandlerMapping mapping;

	@Test
	void everyApiEndpointReturnsResult() {
		List<HandlerMethod> ours = mapping.getHandlerMethods().values().stream()
				.filter(h -> h.getBeanType().getName().startsWith("com.argo."))
				.toList();
		assertFalse(ours.isEmpty());
		List<String> bad = ours.stream().filter(h -> !Result.class.equals(h.getMethod().getReturnType()))
				.map(HandlerMethod::toString).toList();
		assertTrue(bad.isEmpty(), "endpoints not returning Result: " + bad);
	}
}
