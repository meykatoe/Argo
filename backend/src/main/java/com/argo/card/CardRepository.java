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

	// 單一語句扣庫存，不足時不更新
	@Transactional
	@Modifying(flushAutomatically = true)
	@Query("update Card c set c.stock = c.stock - :q where c.id = :id and c.stock >= :q and c.salePrice > 0")
	int decrementStock(@Param("id") Long id, @Param("q") int quantity);

	@Transactional
	@Modifying(flushAutomatically = true)
	@Query("update Card c set c.stock = c.stock + :q where c.id = :id")
	int incrementStock(@Param("id") Long id, @Param("q") int quantity);

	// 開發用，補庫存給零庫存商品
	@Transactional
	@Modifying
	@Query("update Card c set c.stock = :n where c.stock = 0 and c.salePrice > 0")
	int seedStock(@Param("n") int n);
}
