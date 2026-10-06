package com.argo.customer;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CustomerLoginRequest(
		@NotBlank @Size(max = 200) String email,
		@NotBlank @Size(max = 200) String password) {
}
