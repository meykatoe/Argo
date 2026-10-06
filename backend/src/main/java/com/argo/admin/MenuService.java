package com.argo.admin;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class MenuService {

	private final AdminMenuRepository menus;

	public MenuService(AdminMenuRepository menus) {
		this.menus = menus;
	}

	public boolean hasPermission(StaffRole role, String code) {
		return menus.hasPermission(role, code);
	}

	// 葉節點需被授權才顯示，群組節點至少有一個可見子節點才顯示
	public List<MenuNode> menuFor(StaffRole role, LoginPortal portal) {
		Set<Long> granted = new HashSet<>(menus.findGrantedIds(role, portal));
		Map<Long, List<AdminMenu>> children = new LinkedHashMap<>();
		List<AdminMenu> roots = new ArrayList<>();
		for (AdminMenu m : menus.findEnabled(portal)) {
			if (m.getParentId() == null) {
				roots.add(m);
			} else {
				children.computeIfAbsent(m.getParentId(), k -> new ArrayList<>()).add(m);
			}
		}
		return build(roots, children, granted);
	}

	private List<MenuNode> build(List<AdminMenu> nodes, Map<Long, List<AdminMenu>> children,
			Set<Long> granted) {
		List<MenuNode> out = new ArrayList<>();
		for (AdminMenu m : nodes) {
			List<AdminMenu> kids = children.get(m.getId());
			if (kids == null) {
				if (granted.contains(m.getId())) {
					out.add(new MenuNode(m.getCode(), m.getTitle(), m.getPath(), List.of()));
				}
				continue;
			}
			List<MenuNode> sub = build(kids, children, granted);
			if (!sub.isEmpty()) {
				out.add(new MenuNode(m.getCode(), m.getTitle(), m.getPath(), sub));
			}
		}
		return out;
	}
}
