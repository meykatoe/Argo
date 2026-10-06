package com.argo.admin;

// 兩個後台入口，帳號只能從自己的入口登入
public enum LoginPortal {
	ADMIN,
	OPS;

	public boolean accepts(StaffRole role) {
		return (this == OPS) == (role == StaffRole.OPS);
	}
}
