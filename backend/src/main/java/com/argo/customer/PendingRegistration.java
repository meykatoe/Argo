package com.argo.customer;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

@Entity
@Table(name = "customer_pending_registration")
class PendingRegistration {

	@Id
	private String tokenHash;

	@Column(nullable = false)
	private String username;

	@Column(nullable = false)
	private String email;

	@Column(nullable = false)
	private String passwordHash;

	private String name;

	@Column(nullable = false)
	private OffsetDateTime expiresAt;

	@Column(nullable = false)
	private String codeHash;

	@Column(nullable = false)
	private OffsetDateTime codeExpiresAt;

	@Column(nullable = false)
	private int codeAttempts;

	@Column(nullable = false)
	private OffsetDateTime createdAt = OffsetDateTime.now();

	protected PendingRegistration() {
	}

	PendingRegistration(String tokenHash, String username, String email, String passwordHash, String name,
			OffsetDateTime expiresAt, String codeHash, OffsetDateTime codeExpiresAt) {
		this.tokenHash = tokenHash;
		this.username = username;
		this.email = email;
		this.passwordHash = passwordHash;
		this.name = name;
		this.expiresAt = expiresAt;
		this.codeHash = codeHash;
		this.codeExpiresAt = codeExpiresAt;
	}

	// 換新令牌與驗證碼重寄，資料不變，錯誤次數歸零
	PendingRegistration renewed(String newTokenHash, OffsetDateTime newExpiresAt, String newCodeHash,
			OffsetDateTime newCodeExpiresAt) {
		return new PendingRegistration(newTokenHash, username, email, passwordHash, name, newExpiresAt, newCodeHash,
				newCodeExpiresAt);
	}

	// 回傳目前累計的錯誤次數
	int recordCodeFailure() {
		return ++codeAttempts;
	}

	String getCodeHash() {
		return codeHash;
	}

	OffsetDateTime getCodeExpiresAt() {
		return codeExpiresAt;
	}

	String getUsername() {
		return username;
	}

	String getEmail() {
		return email;
	}

	String getPasswordHash() {
		return passwordHash;
	}

	String getName() {
		return name;
	}

	OffsetDateTime getExpiresAt() {
		return expiresAt;
	}

	OffsetDateTime getCreatedAt() {
		return createdAt;
	}
}
