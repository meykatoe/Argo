package com.argo.security.block;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

@Entity
@Table(name = "ip_allowlist")
public class IpAllowEntry {

	@Id
	private String ip;

	@Column(nullable = false)
	private String note;

	@Column(nullable = false)
	private String createdBy;

	@Column(nullable = false)
	private OffsetDateTime createdAt = OffsetDateTime.now();

	protected IpAllowEntry() {
	}

	public IpAllowEntry(String ip, String note, String createdBy) {
		this.ip = ip;
		this.note = note;
		this.createdBy = createdBy;
	}

	public String getIp() {
		return ip;
	}

	public String getNote() {
		return note;
	}

	public String getCreatedBy() {
		return createdBy;
	}

	public OffsetDateTime getCreatedAt() {
		return createdAt;
	}
}
