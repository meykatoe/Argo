package com.argo.customer;

import java.time.OffsetDateTime;

public record CustomerAuthView(String token, String email, String name, OffsetDateTime expiresAt) {
}
