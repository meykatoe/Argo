package com.argo.admin;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

// 各後台只取得自己入口的選單
@RestController
public class MenuController {

	private final MenuService menus;

	public MenuController(MenuService menus) {
		this.menus = menus;
	}

	@GetMapping("/api/admin/menu")
	@AnyStaff
	public List<MenuNode> adminMenu(HttpServletRequest req) {
		return menus.menuFor(staff(req).getRole(), LoginPortal.ADMIN);
	}

	@GetMapping("/api/ops/menu")
	@AnyStaff
	public List<MenuNode> opsMenu(HttpServletRequest req) {
		return menus.menuFor(staff(req).getRole(), LoginPortal.OPS);
	}

	private static StaffAccount staff(HttpServletRequest req) {
		return (StaffAccount) req.getAttribute(AdminAuthInterceptor.STAFF_ATTR);
	}
}
