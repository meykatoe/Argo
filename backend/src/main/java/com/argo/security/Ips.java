package com.argo.security;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.regex.Pattern;

// IP 字串處理，只接受 IP 字面值，絕不做網域查詢
public final class Ips {

	private static final Pattern V4 = Pattern.compile(
			"^(25[0-5]|2[0-4]\\d|1?\\d?\\d)(\\.(25[0-5]|2[0-4]\\d|1?\\d?\\d)){3}$");
	private static final Pattern V6 = Pattern.compile("^[0-9a-fA-F:.]+$");

	private Ips() {
	}

	// 轉成統一寫法，不合法回傳 null
	public static String normalize(String raw) {
		InetAddress a = parse(raw);
		return a == null ? null : a.getHostAddress();
	}

	static InetAddress parse(String raw) {
		if (raw == null) {
			return null;
		}
		String s = raw.trim();
		int scope = s.indexOf('%');
		if (scope >= 0) {
			s = s.substring(0, scope);
		}
		if (s.isEmpty() || s.length() > 45) {
			return null;
		}
		boolean v4 = V4.matcher(s).matches();
		boolean v6 = s.indexOf(':') >= 0 && V6.matcher(s).matches();
		if (!v4 && !v6) {
			return null;
		}
		try {
			return InetAddress.getByName(s);
		} catch (UnknownHostException | SecurityException e) {
			return null;
		}
	}

	// 本機、未指定、連結本地、群播位址不可被封鎖
	public static boolean isSpecial(String ip) {
		InetAddress a = parse(ip);
		return a == null || a.isLoopbackAddress() || a.isAnyLocalAddress() || a.isLinkLocalAddress()
				|| a.isMulticastAddress();
	}

	// 規則可以是單一 IP 或 CIDR，例如 10.0.0.0/8
	public static boolean matches(String ip, String rule) {
		InetAddress a = parse(ip);
		if (a == null || rule == null) {
			return false;
		}
		String r = rule.trim();
		int slash = r.indexOf('/');
		if (slash < 0) {
			InetAddress b = parse(r);
			return b != null && java.util.Arrays.equals(a.getAddress(), b.getAddress());
		}
		InetAddress base = parse(r.substring(0, slash));
		if (base == null) {
			return false;
		}
		int bits;
		try {
			bits = Integer.parseInt(r.substring(slash + 1));
		} catch (NumberFormatException e) {
			return false;
		}
		byte[] x = a.getAddress();
		byte[] y = base.getAddress();
		if (x.length != y.length || bits < 0 || bits > x.length * 8) {
			return false;
		}
		for (int i = 0; i < bits; i++) {
			int mask = 0x80 >> (i % 8);
			if ((x[i / 8] & mask) != (y[i / 8] & mask)) {
				return false;
			}
		}
		return true;
	}

	// 白名單規則：單一 IP 或 CIDR，回傳統一寫法，不合法或範圍大到離譜回傳 null
	public static String normalizeRule(String raw) {
		if (raw == null) {
			return null;
		}
		String r = raw.trim();
		int slash = r.indexOf('/');
		if (slash < 0) {
			return normalize(r);
		}
		InetAddress base = parse(r.substring(0, slash));
		if (base == null) {
			return null;
		}
		int bits;
		try {
			bits = Integer.parseInt(r.substring(slash + 1));
		} catch (NumberFormatException e) {
			return null;
		}
		boolean v4 = base instanceof Inet4Address;
		// 網段太大等於整個網際網路都免檢查，擋掉
		int min = v4 ? 8 : 32;
		int max = v4 ? 32 : 128;
		if (bits < min || bits > max) {
			return null;
		}
		return base.getHostAddress() + "/" + bits;
	}

	public static boolean isV4(String ip) {
		return parse(ip) instanceof Inet4Address;
	}
}
