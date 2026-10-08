package com.argo.order;

import jakarta.persistence.LockModeType;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ShopOrderRepository
		extends JpaRepository<ShopOrder, Long>, JpaSpecificationExecutor<ShopOrder> {

	boolean existsByOrderNo(String orderNo);

	Optional<ShopOrder> findByOrderNo(String orderNo);

	// 鎖定訂單，避免同時付款或取消
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select o from ShopOrder o where o.orderNo = :orderNo")
	Optional<ShopOrder> findByOrderNoForUpdate(@Param("orderNo") String orderNo);

	// 顧客自己的訂單，新的在前
	Page<ShopOrder> findByCustomerId(Long customerId, Pageable pageable);

	List<ShopOrder> findByStatusAndCreatedAtBefore(OrderStatus status, OffsetDateTime before);
}
