package com.argo.customer;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

@Entity
@Table(name = "customer_session")
public class CustomerSession {

	@Id
	private String tokenHash;

	@Column(nullable = false)
	private Long customerId;

	@Column(nullable = false)
	private OffsetDateTime expiresAt;

	@Column(nullable = false)
	private OffsetDateTime createdAt = OffsetDateTime.now();

	protected CustomerSession() {
	}

	public CustomerSession(String tokenHash, Long customerId, OffsetDateTime expiresAt) {
		this.tokenHash = tokenHash;
		this.customerId = customerId;
		this.expiresAt = expiresAt;
	}

	public Long getCustomerId() {
		return customerId;
	}

	public OffsetDateTime getExpiresAt() {
		return expiresAt;
	}
}
