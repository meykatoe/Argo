package com.argo.customer;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record VerifyCodeRequest(@NotBlank @Email @Size(max = 200) String email,
		@NotBlank @Pattern(regexp = "\\s*\\d{6}\\s*") String code) {
}
