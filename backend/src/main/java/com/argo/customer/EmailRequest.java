package com.argo.customer;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EmailRequest(@NotBlank @Email @Size(max = 200) String email) {
}
