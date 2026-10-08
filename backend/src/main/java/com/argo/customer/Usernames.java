package com.argo.customer;

import java.util.Locale;
import java.util.regex.Pattern;

// 帳號名稱規則，不含 @，才能和 Email 區分
final class Usernames {

	private static final Pattern VALID = Pattern.compile("^[a-z0-9][a-z0-9._-]{2,29}$");

	private Usernames() {
	}

	static String normalize(String s) {
		return s == null ? "" : s.trim().toLowerCase(Locale.ROOT);
	}

	static boolean valid(String normalized) {
		return VALID.matcher(normalized).matches();
	}
}
