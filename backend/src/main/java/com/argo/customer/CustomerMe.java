package com.argo.customer;

import com.argo.common.Flag;

public record CustomerMe(String email, String username, String name, int emailVerified) {

	public static CustomerMe from(CustomerAccount c) {
		return new CustomerMe(c.getEmail(), c.getUsername(), c.getName(), Flag.of(c.isEmailVerified()));
	}
}
