package com.argo.customer;

import java.time.OffsetDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface CustomerEmailTokenRepository extends JpaRepository<CustomerEmailToken, String> {

	Optional<CustomerEmailToken> findByTokenHashAndPurpose(String tokenHash, EmailTokenPurpose purpose);

	Optional<CustomerEmailToken> findFirstByCustomerIdAndPurposeOrderByCreatedAtDesc(Long customerId,
			EmailTokenPurpose purpose);

	@Modifying
	@Query("delete from CustomerEmailToken t where t.customerId = :id and t.purpose = :purpose")
	int deleteByCustomer(@Param("id") Long customerId, @Param("purpose") EmailTokenPurpose purpose);

	@Modifying
	@Query("delete from CustomerEmailToken t where t.expiresAt < :now")
	int deleteExpired(@Param("now") OffsetDateTime now);
}
