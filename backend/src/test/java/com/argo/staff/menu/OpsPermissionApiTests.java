package com.argo.staff.menu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.argo.staff.auth.LoginPortal;
import com.argo.staff.auth.StaffAuthService;
import com.argo.staff.auth.StaffRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
@Transactional
class OpsPermissionApiTests {

	static final String PW = "Password-1234";

	@Autowired
	WebApplicationContext wac;
	@Autowired
	StaffAuthService auth;
	@Autowired
	JdbcTemplate jdbc;
	@Autowired
	MenuService menus;

	MockMvc mvc;
	String ops;

	@BeforeEach
	void setUp() {
		mvc = MockMvcBuilders.webAppContextSetup(wac).build();
		auth.create("perm-ops", PW, StaffRole.OPS);
		ops = "Bearer " + auth.login("perm-ops", PW, LoginPortal.OPS).token();
	}

	private ResultActions grant(String code, String role, int granted) throws Exception {
		return mvc.perform(put("/api/ops/permissions/" + code + "/roles/" + role).header("Authorization", ops)
				.contentType(MediaType.APPLICATION_JSON).content("{\"granted\":" + granted + "}"));
	}

	private ResultActions enable(String code, int enabled) throws Exception {
		return mvc.perform(patch("/api/ops/permissions/" + code).header("Authorization", ops)
				.contentType(MediaType.APPLICATION_JSON).content("{\"enabled\":" + enabled + "}"));
	}

	private int audits(String action, String target) {
		return jdbc.queryForObject("select count(*) from staff_audit_log where username = 'perm-ops' "
				+ "and action = ? and target_id = ?", Integer.class, action, target);
	}

	@Test
	void onlyOpsWithTheMenuCanUseIt() throws Exception {
		auth.create("perm-adm", PW, StaffRole.ADMIN);
		String h = "Bearer " + auth.login("perm-adm", PW, LoginPortal.ADMIN).token();
		mvc.perform(get("/api/ops/permissions").header("Authorization", h)).andExpect(status().isForbidden());
		mvc.perform(get("/api/ops/permissions")).andExpect(status().isUnauthorized());
	}

	@Test
	void overviewListsRolesAndNodes() throws Exception {
		mvc.perform(get("/api/ops/permissions").header("Authorization", ops)).andExpect(status().isOk())
				.andExpect(jsonPath("$.data.roles.length()").value(4))
				.andExpect(jsonPath("$.data.nodes[?(@.code == 'order.manage' && 'SERVICE' in @.roles)]").isNotEmpty())
				.andExpect(jsonPath("$.data.nodes[?(@.code == 'system' && @.group == 1)]").isNotEmpty())
				.andExpect(jsonPath("$.data.nodes[?(@.code == 'system.monitor' && @.parentCode == 'system')]").isNotEmpty());
	}

	@Test
	void grantAndRevokeTakeEffectAtOnceAndAreAudited() throws Exception {
		grant("card.edit", "SERVICE", 1).andExpect(status().isOk());
		assertEquals(true, menus.hasPermission(StaffRole.SERVICE, "card.edit"));
		assertEquals(1, audits("PERMISSION_GRANTED", "card.edit"));
		grant("card.edit", "SERVICE", 1).andExpect(status().isOk());
		assertEquals(1, audits("PERMISSION_GRANTED", "card.edit"));
		grant("card.edit", "SERVICE", 0).andExpect(status().isOk());
		assertEquals(false, menus.hasPermission(StaffRole.SERVICE, "card.edit"));
		assertEquals(1, audits("PERMISSION_REVOKED", "card.edit"));
	}

	@Test
	void groupsAndWrongPortalRolesAreRefused() throws Exception {
		grant("card", "SERVICE", 1).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value(2012));
		grant("card.edit", "OPS", 1).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value(2013));
		grant("security.ips", "GENERAL", 1).andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value(2013));
		grant("no.such", "ADMIN", 1).andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value(2011));
		grant("card.edit", "BOSS", 1).andExpect(status().isBadRequest());
	}

	@Test
	void protectedNodesCannotLockOpsOut() throws Exception {
		grant("system.permissions", "OPS", 0).andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value(2014));
		enable("system.permissions", 0).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value(2014));
		enable("system", 0).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value(2014));
		assertEquals(true, menus.hasPermission(StaffRole.OPS, "system.permissions"));
	}

	@Test
	void disablingAMenuHidesAndDeniesItAndIsAudited() throws Exception {
		enable("card.edit", 0).andExpect(status().isOk());
		assertEquals(false, menus.hasPermission(StaffRole.GENERAL, "card.edit"));
		assertEquals(1, audits("MENU_DISABLED", "card.edit"));
		enable("card.edit", 1).andExpect(status().isOk());
		assertEquals(true, menus.hasPermission(StaffRole.GENERAL, "card.edit"));
		assertEquals(1, audits("MENU_ENABLED", "card.edit"));
	}

	@Test
	void flagsMustBeZeroOrOne() throws Exception {
		grant("card.edit", "SERVICE", 2).andExpect(status().isBadRequest());
		enable("card.edit", 5).andExpect(status().isBadRequest());
	}
}
