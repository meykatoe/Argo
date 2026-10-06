package com.argo.security;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "ip_auto_block_rule")
public class IpAutoBlockRule {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, unique = true)
	private AutoBlockMetric metric;

	// 0 停用、1 啟用
	@JdbcTypeCode(SqlTypes.SMALLINT)
	private boolean enabled = true;

	@Column(nullable = false)
	private int threshold;

	@Column(nullable = false)
	private int windowMinutes;

	@Column(nullable = false)
	private int blockHours;

	@Column(nullable = false)
	private OffsetDateTime updatedAt = OffsetDateTime.now();

	private String updatedBy;

	protected IpAutoBlockRule() {
	}

	public AutoBlockMetric getMetric() {
		return metric;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public int getThreshold() {
		return threshold;
	}

	public int getWindowMinutes() {
		return windowMinutes;
	}

	public int getBlockHours() {
		return blockHours;
	}

	public OffsetDateTime getUpdatedAt() {
		return updatedAt;
	}

	public String getUpdatedBy() {
		return updatedBy;
	}

	public void update(boolean enabled, int threshold, int windowMinutes, int blockHours, String by) {
		this.enabled = enabled;
		this.threshold = threshold;
		this.windowMinutes = windowMinutes;
		this.blockHours = blockHours;
		this.updatedBy = by;
		this.updatedAt = OffsetDateTime.now();
	}
}
