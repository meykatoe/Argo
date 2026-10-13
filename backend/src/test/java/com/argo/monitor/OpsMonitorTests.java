package com.argo.monitor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.argo.staff.auth.LoginPortal;
import com.argo.staff.auth.StaffAuthService;
import com.argo.staff.auth.StaffRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
@Transactional
class OpsMonitorTests {

	@Autowired
	WebApplicationContext wac;
	@Autowired
	StaffAuthService auth;
	@Autowired
	RequestStats stats;
	@Autowired
	SystemSampler sampler;
	@Autowired
	MonitorFilter filter;

	MockMvc mvc;
	String ops;
	String admin;

	@BeforeEach
	void setUp() {
		mvc = MockMvcBuilders.webAppContextSetup(wac).addFilters(filter).build();
		auth.create("monops1", "Password-1234", StaffRole.OPS);
		auth.create("monadm1", "Password-1234", StaffRole.ADMIN);
		ops = "Bearer " + auth.login("monops1", "Password-1234", LoginPortal.OPS).token();
		admin = "Bearer " + auth.login("monadm1", "Password-1234", LoginPortal.ADMIN).token();
	}

	long pings(String app) {
		var a = stats.snapshot(5).get(new RequestStats.Key(app, "GET", "/api/ping"));
		return a == null ? 0 : a.count;
	}

	@Test
	void opsSeesSystemNumbers() throws Exception {
		sampler.sample();
		mvc.perform(get("/api/ops/monitor").header("Authorization", ops))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.code").value(200))
				.andExpect(jsonPath("$.data.system.heapMaxMb").isNumber())
				.andExpect(jsonPath("$.data.system.poolMax").value(org.hamcrest.Matchers.greaterThan(0)))
				.andExpect(jsonPath("$.data.level").isString())
				.andExpect(jsonPath("$.data.windowMinutes").value(5));
	}

	@Test
	void windowIsClamped() throws Exception {
		mvc.perform(get("/api/ops/monitor?minutes=9999").header("Authorization", ops))
				.andExpect(jsonPath("$.data.windowMinutes").value(60));
		mvc.perform(get("/api/ops/monitor?minutes=0").header("Authorization", ops))
				.andExpect(jsonPath("$.data.windowMinutes").value(1));
	}

	@Test
	void needsTheOpsPermission() throws Exception {
		mvc.perform(get("/api/ops/monitor")).andExpect(status().isUnauthorized());
		mvc.perform(get("/api/ops/monitor").header("Authorization", admin))
				.andExpect(status().is4xxClientError());
	}

	@Test
	void requestsAreCountedPerFrontend() throws Exception {
		long adminBefore = pings("admin");
		long unknownBefore = pings("unknown");
		mvc.perform(get("/api/ping").header("X-Argo-App", "admin")).andExpect(status().isOk());
		mvc.perform(get("/api/ping").header("X-Argo-App", "evil-app")).andExpect(status().isOk());
		mvc.perform(get("/api/ping")).andExpect(status().isOk());
		assertEquals(adminBefore + 1, pings("admin"));
		assertEquals(unknownBefore + 2, pings("unknown"));
	}

	@Test
	void monitorPollingIsNotCounted() throws Exception {
		var before = stats.snapshot(5).keySet().stream().filter(k -> k.path().contains("monitor")).count();
		mvc.perform(get("/api/ops/monitor").header("Authorization", ops).header("X-Argo-App", "ops"))
				.andExpect(status().isOk());
		var after = stats.snapshot(5).keySet().stream().filter(k -> k.path().contains("monitor")).count();
		assertEquals(before, after);
	}
}
