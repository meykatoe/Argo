package com.argo.admin;

import java.time.OffsetDateTime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StaffSessionRepository extends JpaRepository<StaffSession, String> {

	@Modifying
	@Query("delete from StaffSession s where s.expiresAt < :now")
	int deleteExpired(@Param("now") OffsetDateTime now);
}
