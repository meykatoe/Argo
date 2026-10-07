package com.argo.common;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ErrorCodeTests {

	@Test
	void numbersAreUnique() {
		Set<Integer> seen = new HashSet<>();
		for (ErrorCode c : ErrorCode.values()) {
			assertTrue(seen.add(c.number()), "duplicate " + c.number());
		}
	}

	@Test
	void numbersAreFourDigits() {
		Arrays.stream(ErrorCode.values())
				.forEach(c -> assertTrue(c.number() >= 1000 && c.number() <= 9999, c.name()));
	}

	@Test
	void numbersAreSortedWithinEnum() {
		int prev = 0;
		for (ErrorCode c : ErrorCode.values()) {
			assertTrue(c.number() > prev, c.name() + " out of order");
			prev = c.number();
		}
	}

	@Test
	void errorStatusIsNeverSuccess() {
		Arrays.stream(ErrorCode.values()).forEach(c -> assertTrue(c.status().isError(), c.name()));
	}
}
