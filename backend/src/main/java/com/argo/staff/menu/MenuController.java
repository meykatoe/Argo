package com.argo.staff.menu;

import com.argo.common.Result;
import com.argo.staff.auth.AdminAuthInterceptor;
import com.argo.staff.auth.AnyStaff;
import com.argo.staff.auth.LoginPortal;
import com.argo.staff.auth.StaffAccount;
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
	public Result<List<MenuNode>> adminMenu(HttpServletRequest req) {
		return Result.ok(menus.menuFor(staff(req).getRole(), LoginPortal.ADMIN));
	}

	@GetMapping("/api/ops/menu")
	@AnyStaff
	public Result<List<MenuNode>> opsMenu(HttpServletRequest req) {
		return Result.ok(menus.menuFor(staff(req).getRole(), LoginPortal.OPS));
	}

	private static StaffAccount staff(HttpServletRequest req) {
		return (StaffAccount) req.getAttribute(AdminAuthInterceptor.STAFF_ATTR);
	}
}
