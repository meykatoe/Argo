package com.argo.admin;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "staff_account")
public class StaffAccount {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true)
	private String username;

	@Column(nullable = false)
	private String passwordHash;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private StaffRole role;

	@Column(nullable = false)
	@JdbcTypeCode(SqlTypes.SMALLINT)
	private boolean enabled = true;

	@Column(nullable = false)
	private int failedAttempts;

	private OffsetDateTime lockedUntil;

	@Column(nullable = false)
	private OffsetDateTime createdAt = OffsetDateTime.now();

	private OffsetDateTime lastLoginAt;

	protected StaffAccount() {
	}

	public StaffAccount(String username, String passwordHash, StaffRole role) {
		this.username = username;
		this.passwordHash = passwordHash;
		this.role = role;
	}

	public Long getId() {
		return id;
	}

	public String getUsername() {
		return username;
	}

	public String getPasswordHash() {
		return passwordHash;
	}

	public StaffRole getRole() {
		return role;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public boolean isLocked(OffsetDateTime now) {
		return lockedUntil != null && lockedUntil.isAfter(now);
	}

	// 失敗達上限即鎖定
	public void recordFailure(int maxFailures, OffsetDateTime lockUntil) {
		this.failedAttempts++;
		if (failedAttempts >= maxFailures) {
			this.lockedUntil = lockUntil;
			this.failedAttempts = 0;
		}
	}

	public void recordSuccess(OffsetDateTime now) {
		this.failedAttempts = 0;
		this.lockedUntil = null;
		this.lastLoginAt = now;
	}
}
