package com.argo.staff.auth;

import java.time.OffsetDateTime;

public record LoginResponse(String token, String username, StaffRole role, OffsetDateTime expiresAt) {
}
