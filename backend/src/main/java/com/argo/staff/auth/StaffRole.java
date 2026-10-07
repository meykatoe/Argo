package com.argo.staff.auth;

// 各角色可用功能由 role_menu 決定
public enum StaffRole {
	ADMIN,
	GENERAL,
	SERVICE,
	// 運維，只能使用運維後台
	OPS
}
