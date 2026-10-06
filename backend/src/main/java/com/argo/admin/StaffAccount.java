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
	// 0 正常使用、1 停用
	private boolean disabled;

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

	public boolean isDisabled() {
		return disabled;
	}

	public void setDisabled(boolean disabled) {
		this.disabled = disabled;
	}

	public boolean isLocked(OffsetDateTime now) {
		return lockedUntil != null && lockedUntil.isAfter(now);
	}

	// 允許失敗 allowed 次，超過就鎖定，回傳這次是否剛被鎖定
	public boolean recordFailure(int allowed, OffsetDateTime lockUntil) {
		this.failedAttempts++;
		if (failedAttempts > allowed) {
			this.lockedUntil = lockUntil;
			this.failedAttempts = 0;
			return true;
		}
		return false;
	}

	// 還要等幾秒才能再試，至少 1 秒
	public long retryAfterSeconds(OffsetDateTime now) {
		if (lockedUntil == null) {
			return 0;
		}
		return Math.max(1, java.time.Duration.between(now, lockedUntil).toSeconds() + 1);
	}

	public void recordSuccess(OffsetDateTime now) {
		this.failedAttempts = 0;
		this.lockedUntil = null;
		this.lastLoginAt = now;
	}
}
