package com.argo.common;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PasswordPolicyTests {

	@Test
	void staffNeedsUpperLowerDigitAndEightChars() {
		assertTrue(PasswordPolicy.staffOk("Abcdef12"));
		assertFalse(PasswordPolicy.staffOk("Abcde12"));
		assertFalse(PasswordPolicy.staffOk("abcdefg12"));
		assertFalse(PasswordPolicy.staffOk("ABCDEFG12"));
		assertFalse(PasswordPolicy.staffOk("Abcdefgh"));
		assertFalse(PasswordPolicy.staffOk(null));
	}

	@Test
	void customerNeedsLetterAndDigit() {
		assertTrue(PasswordPolicy.customerOk("abcdefg1"));
		assertTrue(PasswordPolicy.customerOk("ABCDEFG1"));
		assertFalse(PasswordPolicy.customerOk("abcdefgh"));
		assertFalse(PasswordPolicy.customerOk("12345678"));
		assertFalse(PasswordPolicy.customerOk("abc1"));
		assertFalse(PasswordPolicy.customerOk("密碼密碼密碼密碼1"));
	}
}
