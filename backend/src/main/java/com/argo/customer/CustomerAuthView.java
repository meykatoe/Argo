package com.argo.customer;

import java.time.OffsetDateTime;

public record CustomerAuthView(String token, String email, String username, String name, OffsetDateTime expiresAt) {
}
