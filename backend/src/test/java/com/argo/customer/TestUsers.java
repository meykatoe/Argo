package com.argo.customer;

import java.util.Locale;

// 測試用，由 Email 推出唯一帳號
public final class TestUsers {

	private TestUsers() {
	}

	public static String of(String email) {
		return "u-" + email.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-");
	}
}
