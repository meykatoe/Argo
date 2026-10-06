package com.argo.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class IpsTests {

	@Test
	void normalizesValidAddresses() {
		assertEquals("203.0.113.9", Ips.normalize(" 203.0.113.9 "));
		assertEquals("1:0:0:0:0:0:0:1", Ips.normalize("1::1"));
		assertEquals("1:0:0:0:0:0:0:1", Ips.normalize("0001:0000::0001"));
		// 對應 IPv4 的 IPv6 寫法視為同一個位址
		assertEquals("203.0.113.9", Ips.normalize("::ffff:203.0.113.9"));
		assertEquals("fe80:0:0:0:0:0:0:1", Ips.normalize("fe80::1%eth0"));
	}

	@Test
	void rejectsAnythingThatIsNotAnIpLiteral() {
		for (String bad : new String[] { null, "", " ", "example.com", "localhost", "256.1.1.1", "1.2.3", "1.2.3.4.5",
				"1.2.3.4/24", "gggg::1", "::1::2", "a".repeat(60), "1.2.3.4;drop", "-1.2.3.4" }) {
			assertNull(Ips.normalize(bad), String.valueOf(bad));
		}
	}

	@Test
	void specialAddressesAreProtected() {
		for (String ip : new String[] { "127.0.0.1", "127.5.5.5", "::1", "0.0.0.0", "169.254.1.1", "224.0.0.1", "fe80::1" }) {
			assertTrue(Ips.isSpecial(ip), ip);
		}
		for (String ip : new String[] { "203.0.113.9", "198.51.100.1", "10.1.2.3", "192.168.1.5", "2001:db8::1" }) {
			assertFalse(Ips.isSpecial(ip), ip);
		}
	}

	@Test
	void matchesSingleIpsAndCidr() {
		assertTrue(Ips.matches("203.0.113.9", "203.0.113.9"));
		assertFalse(Ips.matches("203.0.113.10", "203.0.113.9"));
		assertTrue(Ips.matches("10.20.30.40", "10.0.0.0/8"));
		assertFalse(Ips.matches("11.20.30.40", "10.0.0.0/8"));
		assertTrue(Ips.matches("192.168.1.77", "192.168.1.64/26"));
		assertFalse(Ips.matches("192.168.1.130", "192.168.1.64/26"));
		assertTrue(Ips.matches("2001:db8::5", "2001:db8::/32"));
		assertFalse(Ips.matches("2001:db9::5", "2001:db8::/32"));
		// 位址種類不同、規則壞掉都不會符合
		assertFalse(Ips.matches("203.0.113.9", "2001:db8::/32"));
		assertFalse(Ips.matches("203.0.113.9", "10.0.0.0/99"));
		assertFalse(Ips.matches("203.0.113.9", "10.0.0.0/x"));
		assertFalse(Ips.matches("203.0.113.9", null));
		assertFalse(Ips.matches("not-an-ip", "10.0.0.0/8"));
	}
}
