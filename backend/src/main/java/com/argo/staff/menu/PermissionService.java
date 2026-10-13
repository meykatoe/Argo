package com.argo.staff.menu;

import com.argo.common.ApiException;
import com.argo.common.ErrorCode;
import com.argo.common.Flag;
import com.argo.staff.audit.AuditAction;
import com.argo.staff.audit.AuditLogService;
import com.argo.staff.auth.StaffAccount;
import com.argo.staff.auth.StaffRole;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PermissionService {

	// 停用或撤銷會鎖死本頁
	static final Set<String> PROTECTED = Set.of("system", "system.permissions");

	private final AdminMenuRepository menus;
	private final AuditLogService audit;

	public PermissionService(AdminMenuRepository menus, AuditLogService audit) {
		this.menus = menus;
		this.audit = audit;
	}

	@Transactional(readOnly = true)
	public PermissionView overview() {
		List<AdminMenu> all = menus.findAllOrdered();
		Map<Long, String> codes = new HashMap<>();
		Set<Long> parents = new java.util.HashSet<>();
		for (AdminMenu m : all) {
			codes.put(m.getId(), m.getCode());
			if (m.getParentId() != null) {
				parents.add(m.getParentId());
			}
		}
		List<PermissionView.Node> nodes = all.stream().map(m -> new PermissionView.Node(m.getCode(),
				m.getTitle(), m.getPath(), m.getPortal().name(), codes.get(m.getParentId()),
				m.getSortOrder(), Flag.of(m.isEnabled()), Flag.of(parents.contains(m.getId())),
				PROTECTED.contains(m.getCode()),
				m.getRoles().stream().map(Enum::name).sorted().toList())).toList();
		return new PermissionView(Arrays.stream(StaffRole.values()).map(Enum::name).toList(), nodes);
	}

	@Transactional
	public void setGrant(StaffAccount actor, String code, StaffRole role, boolean granted) {
		AdminMenu m = find(code);
		if (menus.existsByParentId(m.getId())) {
			throw new ApiException(ErrorCode.MENU_NOT_GRANTABLE);
		}
		if (!m.getPortal().accepts(role)) {
			throw new ApiException(ErrorCode.ROLE_PORTAL_MISMATCH);
		}
		if (!granted && role == StaffRole.OPS && PROTECTED.contains(code)) {
			throw new ApiException(ErrorCode.PERMISSION_PROTECTED);
		}
		boolean changed = granted ? m.getRoles().add(role) : m.getRoles().remove(role);
		if (changed) {
			audit.record(actor, null, granted ? AuditAction.PERMISSION_GRANTED : AuditAction.PERMISSION_REVOKED,
					true, "MENU", code, Map.of("role", role.name()));
			menus.flush();
		}
	}

	@Transactional
	public void setEnabled(StaffAccount actor, String code, boolean enabled) {
		AdminMenu m = find(code);
		if (!enabled && PROTECTED.contains(code)) {
			throw new ApiException(ErrorCode.PERMISSION_PROTECTED);
		}
		if (m.isEnabled() != enabled) {
			m.setEnabled(enabled);
			audit.record(actor, null, enabled ? AuditAction.MENU_ENABLED : AuditAction.MENU_DISABLED, true,
					"MENU", code, null);
			menus.flush();
		}
	}

	private AdminMenu find(String code) {
		return menus.findByCode(code).orElseThrow(() -> new ApiException(ErrorCode.MENU_NOT_FOUND));
	}
}
