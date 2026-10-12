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

	// 先寫入再清快取，已刪的登入才不會被讀到
	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query("delete from CustomerSession s where s.customerId = :id")
	int deleteByCustomer(@Param("id") Long customerId);
}
