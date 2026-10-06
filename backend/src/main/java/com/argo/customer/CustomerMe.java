package com.argo.customer;

public record CustomerMe(String email, String name) {

	public static CustomerMe from(CustomerAccount c) {
		return new CustomerMe(c.getEmail(), c.getName());
	}
}
