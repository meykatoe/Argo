package com.argo.customer;

import java.time.OffsetDateTime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CustomerSessionRepository extends JpaRepository<CustomerSession, String> {

	@Modifying
	@Query("delete from CustomerSession s where s.expiresAt < :now")
	int deleteExpired(@Param("now") OffsetDateTime now);
}
