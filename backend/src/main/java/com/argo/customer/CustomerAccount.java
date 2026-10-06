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
