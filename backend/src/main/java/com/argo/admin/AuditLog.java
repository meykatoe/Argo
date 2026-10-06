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
import java.util.Map;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "staff_audit_log")
public class AuditLog {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private Long staffId;

	@Column(nullable = false)
	private String username;

	@Enumerated(EnumType.STRING)
	private StaffRole role;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private AuditAction action;

	private String targetType;

	private String targetId;

	@JdbcTypeCode(SqlTypes.JSON)
	private Map<String, Object> detail;

	@Column(nullable = false)
	private boolean success = true;

	private String ip;

	private String userAgent;

	@Column(nullable = false)
	private OffsetDateTime createdAt = OffsetDateTime.now();

	protected AuditLog() {
	}

	public AuditLog(Long staffId, String username, StaffRole role, AuditAction action,
			boolean success, String targetType, String targetId, Map<String, Object> detail,
			String ip, String userAgent) {
		this.staffId = staffId;
		this.username = username;
		this.role = role;
		this.action = action;
		this.success = success;
		this.targetType = targetType;
		this.targetId = targetId;
		this.detail = detail;
		this.ip = ip;
		this.userAgent = userAgent;
	}

	public Long getId() {
		return id;
	}

	public Long getStaffId() {
		return staffId;
	}

	public String getUsername() {
		return username;
	}

	public StaffRole getRole() {
		return role;
	}

	public AuditAction getAction() {
		return action;
	}

	public String getTargetType() {
		return targetType;
	}

	public String getTargetId() {
		return targetId;
	}

	public Map<String, Object> getDetail() {
		return detail;
	}

	public boolean isSuccess() {
		return success;
	}

	public String getIp() {
		return ip;
	}

	public OffsetDateTime getCreatedAt() {
		return createdAt;
	}
}
