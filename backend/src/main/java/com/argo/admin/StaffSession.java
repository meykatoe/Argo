package com.argo.admin;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

@Entity
@Table(name = "staff_session")
public class StaffSession {

	@Id
	private String tokenHash;

	@Column(nullable = false)
	private Long staffId;

	@Column(nullable = false)
	private OffsetDateTime expiresAt;

	@Column(nullable = false)
	private OffsetDateTime createdAt = OffsetDateTime.now();

	protected StaffSession() {
	}

	public StaffSession(String tokenHash, Long staffId, OffsetDateTime expiresAt) {
		this.tokenHash = tokenHash;
		this.staffId = staffId;
		this.expiresAt = expiresAt;
	}

	public String getTokenHash() {
		return tokenHash;
	}

	public Long getStaffId() {
		return staffId;
	}

	public OffsetDateTime getExpiresAt() {
		return expiresAt;
	}
}
