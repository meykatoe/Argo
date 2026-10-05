package com.argo.order;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "payment")
public class Payment {

	public static final String SUCCEEDED = "SUCCEEDED";
	public static final String FAILED = "FAILED";

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private Long orderId;

	@Column(nullable = false)
	private String method;

	@Column(nullable = false)
	private String status;

	@Column(nullable = false)
	private BigDecimal amount;

	@Column(nullable = false)
	private String currency;

	private String cardLast4;

	private String transactionId;

	private String failureCode;

	@Column(nullable = false)
	private OffsetDateTime createdAt = OffsetDateTime.now();

	protected Payment() {
	}

	public Payment(Long orderId, String method, String status, BigDecimal amount, String currency,
			String cardLast4, String transactionId, String failureCode) {
		this.orderId = orderId;
		this.method = method;
		this.status = status;
		this.amount = amount;
		this.currency = currency;
		this.cardLast4 = cardLast4;
		this.transactionId = transactionId;
		this.failureCode = failureCode;
	}

	public String getStatus() {
		return status;
	}

	public String getCardLast4() {
		return cardLast4;
	}

	public String getFailureCode() {
		return failureCode;
	}

	public OffsetDateTime getCreatedAt() {
		return createdAt;
	}
}
