package com.argo.security;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// hours 不填代表永久封鎖
public record BlockIpRequest(
		@NotBlank @Size(max = 45) String ip,
		@NotBlank @Size(max = 200) String reason,
		@Min(1) @Max(8760) Integer hours) {
}
