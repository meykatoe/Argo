package com.argo.customer;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CustomerAccountRepository extends JpaRepository<CustomerAccount, Long> {

	Optional<CustomerAccount> findByEmail(String email);

	// 登入時鎖住該列，同一帳號的嘗試依序處理，失敗次數才不會漏算
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select a from CustomerAccount a where a.email = :email")
	Optional<CustomerAccount> findByEmailForUpdate(@Param("email") String email);
}
