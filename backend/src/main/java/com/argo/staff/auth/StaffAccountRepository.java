package com.argo.staff.auth;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StaffAccountRepository extends JpaRepository<StaffAccount, Long> {

	Optional<StaffAccount> findByUsername(String username);

	// 登入時鎖住該列，同一帳號的嘗試依序處理，失敗次數才不會漏算
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select a from StaffAccount a where a.username = :username")
	Optional<StaffAccount> findByUsernameForUpdate(@Param("username") String username);
}
