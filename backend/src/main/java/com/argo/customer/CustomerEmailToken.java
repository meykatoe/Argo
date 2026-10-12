package com.argo.customer;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

@Entity
@Table(name = "customer_email_token")
class CustomerEmailToken {

	@Id
	private String tokenHash;

	@Column(nullable = false)
	private Long customerId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private EmailTokenPurpose purpose;

	@Column(nullable = false)
	private OffsetDateTime expiresAt;

	@Column(nullable = false)
	private OffsetDateTime createdAt = OffsetDateTime.now();

	protected CustomerEmailToken() {
	}

	CustomerEmailToken(String tokenHash, Long customerId, EmailTokenPurpose purpose, OffsetDateTime expiresAt) {
		this.tokenHash = tokenHash;
		this.customerId = customerId;
		this.purpose = purpose;
		this.expiresAt = expiresAt;
	}

	Long getCustomerId() {
		return customerId;
	}

	OffsetDateTime getExpiresAt() {
		return expiresAt;
	}

	OffsetDateTime getCreatedAt() {
		return createdAt;
	}
}
