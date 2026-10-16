package com.argo.security.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
class SecurityHeadersFilterTests {

	@Autowired
	WebApplicationContext wac;
	@Autowired
	SecurityHeadersFilter filter;

	MockMvc mvc;

	@BeforeEach
	void setUp() {
		mvc = MockMvcBuilders.webAppContextSetup(wac).addFilters(filter).build();
	}

	@Test
	void apiGetsHeaders() throws Exception {
		mvc.perform(get("/api/ping"))
				.andExpect(header().string("X-Content-Type-Options", "nosniff"))
				.andExpect(header().string("X-Frame-Options", "DENY"))
				.andExpect(header().string("Referrer-Policy", "no-referrer"))
				.andExpect(header().string("Cache-Control", "no-store"))
				.andExpect(header().string("Content-Security-Policy",
						"default-src 'none'; frame-ancestors 'none'"))
				.andExpect(header().doesNotExist("Strict-Transport-Security"));
	}

	@Test
	void hstsOnlyOverHttps() throws Exception {
		mvc.perform(get("/api/ping").header("X-Forwarded-Proto", "https"))
				.andExpect(header().exists("Strict-Transport-Security"));
	}

	@Test
	void nonApiUntouched() throws Exception {
		mvc.perform(get("/actuator/health"))
				.andExpect(header().doesNotExist("X-Frame-Options"));
	}
}
