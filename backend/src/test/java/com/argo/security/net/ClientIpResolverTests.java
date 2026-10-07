package com.argo.security.net;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class ClientIpResolverTests {

	private final ClientIpResolver resolver = new ClientIpResolver("127.0.0.1, ::1, 10.0.0.0/8");

	private MockHttpServletRequest req(String remote, String xff) {
		MockHttpServletRequest r = new MockHttpServletRequest();
		r.setRemoteAddr(remote);
		if (xff != null) {
			r.addHeader("X-Forwarded-For", xff);
		}
		return r;
	}

	@Test
	void directConnectionUsesTheSocketAddress() {
		assertEquals("203.0.113.9", resolver.resolve(req("203.0.113.9", null)));
	}

	@Test
	void forwardedHeaderFromAnUntrustedPeerIsIgnored() {
		// 任何人都能自己加標頭，不能因此冒用別人的 IP
		assertEquals("203.0.113.9", resolver.resolve(req("203.0.113.9", "198.51.100.1")));
	}

	@Test
	void trustedProxyPassesTheRealClient() {
		assertEquals("198.51.100.1", resolver.resolve(req("127.0.0.1", "198.51.100.1")));
		assertEquals("198.51.100.1", resolver.resolve(req("10.1.2.3", "198.51.100.1")));
	}

	@Test
	void takesTheRightmostUntrustedHopSoClientCannotSpoofTheLeftSide() {
		// 用戶端自己塞了假的 1.1.1.1，代理把真實來源附加在右邊
		assertEquals("198.51.100.1", resolver.resolve(req("127.0.0.1", "1.1.1.1, 198.51.100.1")));
		assertEquals("198.51.100.1", resolver.resolve(req("127.0.0.1", "1.1.1.1, 198.51.100.1, 10.0.0.5")));
	}

	@Test
	void garbageOrMissingHeaderFallsBackToTheProxyAddress() {
		assertEquals("127.0.0.1", resolver.resolve(req("127.0.0.1", null)));
		assertEquals("127.0.0.1", resolver.resolve(req("127.0.0.1", "  ")));
		assertEquals("127.0.0.1", resolver.resolve(req("127.0.0.1", "evil.example")));
		assertEquals("127.0.0.1", resolver.resolve(req("127.0.0.1", "198.51.100.1, ???")));
		assertEquals("127.0.0.1", resolver.resolve(req("127.0.0.1", "10.0.0.7")));
	}

	@Test
	void ipv6AndMappedAddressesAreNormalized() {
		assertEquals("2001:db8:0:0:0:0:0:1", resolver.resolve(req("127.0.0.1", "2001:db8::1")));
		assertEquals("203.0.113.9", resolver.resolve(req("::ffff:203.0.113.9", null)));
	}

	@Test
	void nothingIsTrustedByDefault() {
		ClientIpResolver none = new ClientIpResolver("");
		assertEquals("127.0.0.1", none.resolve(req("127.0.0.1", "198.51.100.1")));
	}
}
