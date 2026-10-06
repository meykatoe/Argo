package com.argo.security;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

// 只信任受信任代理轉送的標頭，否則任何人都能偽造來源 IP
@Component
public class ClientIpResolver {

	private final List<String> trusted;

	public ClientIpResolver(@Value("${argo.security.trusted-proxies:}") String trusted) {
		this.trusted = Arrays.stream(trusted.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
	}

	public boolean isTrustedProxy(String ip) {
		return trusted.stream().anyMatch(rule -> Ips.matches(ip, rule));
	}

	public String resolve(HttpServletRequest req) {
		String remote = Ips.normalize(req.getRemoteAddr());
		if (remote == null) {
			return "unknown";
		}
		if (!isTrustedProxy(remote)) {
			return remote;
		}
		String xff = req.getHeader("X-Forwarded-For");
		if (xff == null || xff.isBlank()) {
			return remote;
		}
		// 由右往左，跳過受信任代理，第一個不是代理的就是用戶端
		String[] parts = xff.split(",");
		for (int i = parts.length - 1; i >= 0; i--) {
			String ip = Ips.normalize(parts[i]);
			if (ip == null) {
				return remote;
			}
			if (!isTrustedProxy(ip)) {
				return ip;
			}
		}
		return remote;
	}
}
