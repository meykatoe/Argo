package com.argo.security.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// ip 可以是單一位址或 CIDR，例如 203.0.113.0/24
public record AddAllowRequest(@NotBlank @Size(max = 49) String ip, @NotBlank @Size(max = 200) String note) {
}
