package com.argo.customer;

import java.time.OffsetDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface PendingRegistrationRepository extends JpaRepository<PendingRegistration, String> {

	Optional<PendingRegistration> findFirstByEmailOrderByCreatedAtDesc(String email);

	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query("delete from PendingRegistration p where p.email = :email")
	int deleteByEmail(@Param("email") String email);

	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query("delete from PendingRegistration p where p.expiresAt < :now")
	int deleteExpired(@Param("now") OffsetDateTime now);
}
