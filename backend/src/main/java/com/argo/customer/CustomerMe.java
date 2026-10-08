package com.argo.customer;

public record CustomerMe(String email, String username, String name) {

	public static CustomerMe from(CustomerAccount c) {
		return new CustomerMe(c.getEmail(), c.getUsername(), c.getName());
	}
}
