package com.argo.card;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface CardRepository extends JpaRepository<Card, Long>, JpaSpecificationExecutor<Card> {

	List<Card> findBySourceKeyIn(Collection<String> keys);

	// 開發用，補庫存給零庫存商品
	@Transactional
	@Modifying
	@Query("update Card c set c.stock = :n where c.stock = 0 and c.salePrice > 0")
	int seedStock(@Param("n") int n);
}
