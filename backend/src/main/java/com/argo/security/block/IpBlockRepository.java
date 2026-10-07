package com.argo.security.block;

import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface IpBlockRepository extends JpaRepository<IpBlock, String> {

	@Query("select b from IpBlock b where b.expiresAt is null or b.expiresAt > :now order by b.createdAt desc")
	List<IpBlock> findActive(@Param("now") OffsetDateTime now);

	@Modifying
	@Query("delete from IpBlock b where b.expiresAt is not null and b.expiresAt <= :now")
	int deleteExpired(@Param("now") OffsetDateTime now);
}
