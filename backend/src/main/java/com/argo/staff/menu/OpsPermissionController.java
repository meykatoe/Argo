package com.argo.staff.menu;

import com.argo.common.Result;
import com.argo.staff.auth.AdminAuthInterceptor;
import com.argo.staff.auth.RequirePermission;
import com.argo.staff.auth.StaffAccount;
import com.argo.staff.auth.StaffRole;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ops/permissions")
public class OpsPermissionController {

	private final PermissionService permissions;

	public OpsPermissionController(PermissionService permissions) {
		this.permissions = permissions;
	}

	@GetMapping
	@RequirePermission("system.permissions")
	public Result<PermissionView> overview() {
		return Result.ok(permissions.overview());
	}

	@PutMapping("/{code}/roles/{role}")
	@RequirePermission("system.permissions")
	public Result<PermissionView> grant(@PathVariable String code, @PathVariable StaffRole role,
			@Valid @RequestBody GrantRequest body, HttpServletRequest req) {
		permissions.setGrant(staff(req), code, role, body.granted() == 1);
		return Result.ok(permissions.overview());
	}

	@PatchMapping("/{code}")
	@RequirePermission("system.permissions")
	public Result<PermissionView> enable(@PathVariable String code, @Valid @RequestBody EnabledRequest body,
			HttpServletRequest req) {
		permissions.setEnabled(staff(req), code, body.enabled() == 1);
		return Result.ok(permissions.overview());
	}

	private static StaffAccount staff(HttpServletRequest req) {
		return (StaffAccount) req.getAttribute(AdminAuthInterceptor.STAFF_ATTR);
	}
}
