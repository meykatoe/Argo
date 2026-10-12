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
	private OffsetDateTime createdAt = OffsetDateTime.now();

	protected PendingRegistration() {
	}

	PendingRegistration(String tokenHash, String username, String email, String passwordHash, String name,
			OffsetDateTime expiresAt) {
		this.tokenHash = tokenHash;
		this.username = username;
		this.email = email;
		this.passwordHash = passwordHash;
		this.name = name;
		this.expiresAt = expiresAt;
	}

	// 換新令牌重寄，資料不變
	PendingRegistration withToken(String newTokenHash, OffsetDateTime newExpiresAt) {
		return new PendingRegistration(newTokenHash, username, email, passwordHash, name, newExpiresAt);
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
