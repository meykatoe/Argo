package com.argo.customer;

import java.time.OffsetDateTime;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface PendingRegistrationRepository extends JpaRepository<PendingRegistration, String> {

	Optional<PendingRegistration> findFirstByEmailOrderByCreatedAtDesc(String email);

	// 驗證碼嘗試依序處理，錯誤次數才不會漏算
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select p from PendingRegistration p where p.email = :email order by p.createdAt desc limit 1")
	Optional<PendingRegistration> findLatestForUpdate(@Param("email") String email);

	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query("delete from PendingRegistration p where p.email = :email")
	int deleteByEmail(@Param("email") String email);

	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query("delete from PendingRegistration p where p.expiresAt < :now")
	int deleteExpired(@Param("now") OffsetDateTime now);
}
