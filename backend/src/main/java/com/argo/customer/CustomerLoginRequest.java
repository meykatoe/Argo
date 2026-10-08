package com.argo.customer;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CustomerLoginRequest(
		// 帳號或 Email
		@NotBlank @Size(max = 200) String account,
		@NotBlank @Size(max = 200) String password) {
}
