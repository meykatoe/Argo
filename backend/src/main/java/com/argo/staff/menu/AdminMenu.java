package com.argo.staff.menu;

import com.argo.staff.auth.LoginPortal;
import com.argo.staff.auth.StaffRole;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.util.HashSet;
import java.util.Set;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "admin_menu")
public class AdminMenu {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private Long parentId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private LoginPortal portal;

	@Column(nullable = false, unique = true)
	private String code;

	@Column(nullable = false)
	private String title;

	private String path;

	@Column(nullable = false)
	private int sortOrder;

	@Column(nullable = false)
	@JdbcTypeCode(SqlTypes.SMALLINT)
	private boolean enabled = true;

	@ElementCollection
	@CollectionTable(name = "role_menu", joinColumns = @JoinColumn(name = "menu_id"))
	@Enumerated(EnumType.STRING)
	@Column(name = "role")
	private Set<StaffRole> roles = new HashSet<>();

	protected AdminMenu() {
	}

	public Long getId() {
		return id;
	}

	public Long getParentId() {
		return parentId;
	}

	public String getCode() {
		return code;
	}

	public String getTitle() {
		return title;
	}

	public String getPath() {
		return path;
	}

	public int getSortOrder() {
		return sortOrder;
	}
}
