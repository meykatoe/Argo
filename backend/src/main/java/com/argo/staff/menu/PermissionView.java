package com.argo.staff.menu;

import java.util.List;

// enabled、group 為 0/1
public record PermissionView(List<String> roles, List<Node> nodes) {

	public record Node(String code, String title, String path, String portal, String parentCode,
			int sortOrder, int enabled, int group, boolean protectedNode, List<String> roles) {
	}
}
