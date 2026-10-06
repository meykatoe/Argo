package com.argo.admin;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AdminMenuRepository extends JpaRepository<AdminMenu, Long> {

	@Query("select m from AdminMenu m where m.portal = :portal and m.enabled = :#{true} order by m.sortOrder, m.id")
	List<AdminMenu> findEnabled(@Param("portal") LoginPortal portal);

	// 該角色被授權的節點編號
	@Query("select m.id from AdminMenu m join m.roles r where r = :role and m.portal = :portal and m.enabled = :#{true}")
	List<Long> findGrantedIds(@Param("role") StaffRole role, @Param("portal") LoginPortal portal);

	@Query("select count(m) > 0 from AdminMenu m join m.roles r where r = :role and m.code = :code and m.enabled = :#{true}")
	boolean hasPermission(@Param("role") StaffRole role, @Param("code") String code);
}
