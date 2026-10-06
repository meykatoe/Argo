package com.argo.customer;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "customer_account")
public class CustomerAccount {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true)
	private String email;

	@Column(nullable = false)
	private String passwordHash;

	private String name;

	// 0 正常使用、1 停用
	@JdbcTypeCode(SqlTypes.SMALLINT)
	private boolean disabled;

	@Column(nullable = false)
	private int failedAttempts;

	private OffsetDateTime lockedUntil;

	@Column(nullable = false)
	private OffsetDateTime createdAt = OffsetDateTime.now();

	private OffsetDateTime lastLoginAt;

	protected CustomerAccount() {
	}

	public CustomerAccount(String email, String passwordHash, String name) {
		this.email = email;
		this.passwordHash = passwordHash;
		this.name = name;
	}

	public Long getId() {
		return id;
	}

	public String getEmail() {
		return email;
	}

	public String getPasswordHash() {
		return passwordHash;
	}

	public String getName() {
		return name;
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
