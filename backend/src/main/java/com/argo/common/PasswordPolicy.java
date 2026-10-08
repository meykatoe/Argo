package com.argo.common;

// 密碼強度規則
public final class PasswordPolicy {

	public static final int MIN_LENGTH = 8;

	private PasswordPolicy() {
	}

	// 後台：大寫、小寫、數字各一
	public static boolean staffOk(String p) {
		return p != null && p.length() >= MIN_LENGTH && has(p, 'A', 'Z') && has(p, 'a', 'z') && has(p, '0', '9');
	}

	// 官網：英文字母加數字
	public static boolean customerOk(String p) {
		return p != null && p.length() >= MIN_LENGTH && (has(p, 'A', 'Z') || has(p, 'a', 'z')) && has(p, '0', '9');
	}

	private static boolean has(String s, char from, char to) {
		return s.chars().anyMatch(c -> c >= from && c <= to);
	}
}
