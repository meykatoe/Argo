package com.argo.admin;

import java.time.OffsetDateTime;

public record LoginResponse(String token, String username, StaffRole role, OffsetDateTime expiresAt) {
}
