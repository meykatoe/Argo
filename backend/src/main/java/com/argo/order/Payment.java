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

	public static final String PENDING = "PENDING";
	public static final String SUCCEEDED = "SUCCEEDED";
	public static final String FAILED = "FAILED";

	public static final String REFUND_NONE = "NONE";
	public static final String REFUND_REQUIRED = "REQUIRED";
	public static final String REFUND_REFUNDED = "REFUNDED";

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

	private String tradeNo;

	@Column(nullable = false)
	private String refundStatus = REFUND_NONE;

	private OffsetDateTime refundedAt;

	private Long refundedBy;

	@Column(nullable = false)
	private OffsetDateTime createdAt = OffsetDateTime.now();

	@Column(nullable = false)
	private OffsetDateTime updatedAt = OffsetDateTime.now();

	protected Payment() {
	}

	public static Payment pending(Long orderId, String method, String tradeNo, BigDecimal amount,
			String currency) {
		Payment p = new Payment();
		p.orderId = orderId;
		p.method = method;
		p.status = PENDING;
		p.amount = amount;
		p.currency = currency;
		p.tradeNo = tradeNo;
		return p;
	}

	public void succeed(String transactionId, String cardLast4) {
		this.status = SUCCEEDED;
		this.transactionId = transactionId;
		this.cardLast4 = cardLast4;
		this.updatedAt = OffsetDateTime.now();
	}

	public void fail(String failureCode) {
		this.status = FAILED;
		this.failureCode = failureCode;
		this.updatedAt = OffsetDateTime.now();
	}

	public void requireRefund() {
		this.refundStatus = REFUND_REQUIRED;
		this.updatedAt = OffsetDateTime.now();
	}

	public void markRefunded(Long staffId) {
		this.refundStatus = REFUND_REFUNDED;
		this.refundedAt = OffsetDateTime.now();
		this.refundedBy = staffId;
		this.updatedAt = this.refundedAt;
	}

	public String getMethod() {
		return method;
	}

	public Long getOrderId() {
		return orderId;
	}

	public BigDecimal getAmount() {
		return amount;
	}

	public String getTradeNo() {
		return tradeNo;
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

	public String getRefundStatus() {
		return refundStatus;
	}

	public OffsetDateTime getRefundedAt() {
		return refundedAt;
	}

	public Long getRefundedBy() {
		return refundedBy;
	}

	public OffsetDateTime getCreatedAt() {
		return createdAt;
	}
}
