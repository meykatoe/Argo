package com.argo.security.block;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "ip_block")
public class IpBlock {

	@Id
	private String ip;

	@Column(nullable = false)
	private String reason;

	@Column(nullable = false)
	private String blockedBy;

	private Long staffId;

	@Column(nullable = false)
	private OffsetDateTime createdAt = OffsetDateTime.now();

	// 為空代表永久
	private OffsetDateTime expiresAt;

	// 0 人工、1 系統自動
	@JdbcTypeCode(SqlTypes.SMALLINT)
	private boolean auto;

	protected IpBlock() {
	}

	public IpBlock(String ip, String reason, String blockedBy, Long staffId, OffsetDateTime expiresAt,
			boolean auto) {
		this.auto = auto;
		this.ip = ip;
		this.reason = reason;
		this.blockedBy = blockedBy;
		this.staffId = staffId;
		this.expiresAt = expiresAt;
	}

	public String getIp() {
		return ip;
	}

	public String getReason() {
		return reason;
	}

	public String getBlockedBy() {
		return blockedBy;
	}

	public OffsetDateTime getCreatedAt() {
		return createdAt;
	}

	public OffsetDateTime getExpiresAt() {
		return expiresAt;
	}

	public boolean isAuto() {
		return auto;
	}

	public boolean isActive(OffsetDateTime now) {
		return expiresAt == null || expiresAt.isAfter(now);
	}
}
