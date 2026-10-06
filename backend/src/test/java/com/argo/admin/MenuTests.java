package com.argo.admin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
@Transactional
class MenuTests {

	static final String PW = "password-1234";

	@Autowired
	MenuService menus;
	@Autowired
	StaffAuthService auth;
	@Autowired
	JdbcTemplate jdbc;
	@Autowired
	WebApplicationContext wac;

	MockMvc mvc;

	@BeforeEach
	void setUp() {
		mvc = MockMvcBuilders.webAppContextSetup(wac).build();
	}

	private List<String> codes(List<MenuNode> nodes) {
		return nodes.stream().flatMap(n -> java.util.stream.Stream.concat(
				java.util.stream.Stream.of(n.code()), codes(n.children()).stream())).toList();
	}

	@Test
	void seededMenuMatchesTheCurrentPermissions() {
		assertEquals(List.of("card", "card.edit"), codes(menus.menuFor(StaffRole.GENERAL, LoginPortal.ADMIN)));
		assertEquals(List.of("card", "card.edit"), codes(menus.menuFor(StaffRole.ADMIN, LoginPortal.ADMIN)));
		assertEquals(List.of(), codes(menus.menuFor(StaffRole.SERVICE, LoginPortal.ADMIN)));
		assertEquals(List.of("audit", "audit.logs"), codes(menus.menuFor(StaffRole.OPS, LoginPortal.OPS)));
		assertEquals(List.of(), codes(menus.menuFor(StaffRole.ADMIN, LoginPortal.OPS)));
		assertEquals(List.of(), codes(menus.menuFor(StaffRole.OPS, LoginPortal.ADMIN)));
	}

	@Test
	void groupsCarryTitlesAndLeavesCarryPaths() {
		MenuNode group = menus.menuFor(StaffRole.GENERAL, LoginPortal.ADMIN).get(0);
		assertEquals("卡牌管理", group.title());
		assertEquals(null, group.path());
		assertEquals("卡牌編輯", group.children().get(0).title());
		assertEquals("/cards", group.children().get(0).path());
	}

	@Test
	void newMenuAndGrantWorkWithoutCodeChanges() {
		jdbc.update("insert into admin_menu (parent_id, portal, code, title, path, sort_order) "
				+ "values ((select id from admin_menu where code = 'card'), 'ADMIN', 'card.series', '卡牌系列', '/series', 5)");
		jdbc.update("insert into role_menu (role, menu_id) select 'SERVICE', id from admin_menu where code = 'card.series'");
		List<MenuNode> svc = menus.menuFor(StaffRole.SERVICE, LoginPortal.ADMIN);
		assertEquals(List.of("card", "card.series"), codes(svc));
		// 排序小的在前
		List<MenuNode> gen = menus.menuFor(StaffRole.GENERAL, LoginPortal.ADMIN);
		assertEquals(List.of("card", "card.edit"), codes(gen));
		assertTrue(menus.hasPermission(StaffRole.SERVICE, "card.series"));
		assertFalse(menus.hasPermission(StaffRole.GENERAL, "card.series"));
	}

	@Test
	void sortOrderControlsPosition() {
		jdbc.update("insert into admin_menu (parent_id, portal, code, title, path, sort_order) "
				+ "values ((select id from admin_menu where code = 'card'), 'ADMIN', 'card.series', '卡牌系列', '/series', 5)");
		jdbc.update("insert into role_menu (role, menu_id) select 'GENERAL', id from admin_menu where code = 'card.series'");
		var kids = menus.menuFor(StaffRole.GENERAL, LoginPortal.ADMIN).get(0).children();
		assertEquals(List.of("card.series", "card.edit"), kids.stream().map(MenuNode::code).toList());
	}

	@Test
	void disabledNodeIsHiddenAndDenied() {
		jdbc.update("update admin_menu set enabled = false where code = 'card.edit'");
		assertEquals(List.of(), codes(menus.menuFor(StaffRole.GENERAL, LoginPortal.ADMIN)));
		assertFalse(menus.hasPermission(StaffRole.GENERAL, "card.edit"));
	}

	@Test
	void revokingAGrantTakesEffectImmediately() throws Exception {
		auth.create("gen1", PW, StaffRole.GENERAL);
		String h = "Bearer " + auth.login("gen1", PW, LoginPortal.ADMIN).token();
		mvc.perform(get("/api/admin/cards").header("Authorization", h)).andExpect(status().isOk());
		jdbc.update("delete from role_menu where role = 'GENERAL'");
		mvc.perform(get("/api/admin/cards").header("Authorization", h)).andExpect(status().isForbidden());
	}

	@Test
	void menuEndpointFollowsRoleAndPortal() throws Exception {
		auth.create("gen1", PW, StaffRole.GENERAL);
		auth.create("svc1", PW, StaffRole.SERVICE);
		auth.create("ops1", PW, StaffRole.OPS);
		String gen = "Bearer " + auth.login("gen1", PW, LoginPortal.ADMIN).token();
		String svc = "Bearer " + auth.login("svc1", PW, LoginPortal.ADMIN).token();
		String ops = "Bearer " + auth.login("ops1", PW, LoginPortal.OPS).token();
		mvc.perform(get("/api/admin/menu").header("Authorization", gen)).andExpect(status().isOk())
				.andExpect(jsonPath("$.data[0].title").value("卡牌管理"))
				.andExpect(jsonPath("$.data[0].children[0].path").value("/cards"));
		mvc.perform(get("/api/admin/menu").header("Authorization", svc)).andExpect(status().isOk())
				.andExpect(jsonPath("$.data.length()").value(0));
		mvc.perform(get("/api/ops/menu").header("Authorization", ops)).andExpect(status().isOk())
				.andExpect(jsonPath("$.data[0].children[0].code").value("audit.logs"));
		mvc.perform(get("/api/ops/menu")).andExpect(status().isUnauthorized());
	}

	@Test
	void endpointWithoutAnnotationIsDenied() throws Exception {
		auth.create("adm1", PW, StaffRole.ADMIN);
		String h = "Bearer " + auth.login("adm1", PW, LoginPortal.ADMIN).token();
		mvc.perform(get("/api/admin/does-not-exist").header("Authorization", h))
				.andExpect(status().isForbidden());
	}
}
