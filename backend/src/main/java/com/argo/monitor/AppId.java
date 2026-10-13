package com.argo.monitor;

import java.util.Set;

// 白名單
final class AppId {

	static final String HEADER = "X-Argo-App";
	static final String UNKNOWN = "unknown";

	private static final Set<String> KNOWN = Set.of("official-site", "admin", "ops");

	private AppId() {
	}

	static String of(String raw) {
		String v = raw == null ? "" : raw.trim();
		return KNOWN.contains(v) ? v : UNKNOWN;
	}
}
