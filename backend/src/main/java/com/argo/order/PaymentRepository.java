package com.argo.order;

import jakarta.persistence.LockModeType;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

	Optional<Payment> findByTradeNo(String tradeNo);

	Page<Payment> findByRefundStatus(String refundStatus, Pageable pageable);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select p from Payment p where p.id = :id")
	Optional<Payment> findByIdForUpdate(@Param("id") Long id);

	long countByOrderId(Long orderId);

	Optional<Payment> findFirstByOrderIdOrderByIdDesc(Long orderId);

	// 還在等付款的紀錄：已超過等待時間，或訂單即將過期
	@Query("select p from Payment p, ShopOrder o where o.id = p.orderId and p.status = 'PENDING' "
			+ "and p.method = :method and o.status = :orderStatus "
			+ "and (p.createdAt < :paymentBefore or o.createdAt < :orderBefore) order by p.id")
	List<Payment> findReconcilable(@Param("method") String method, @Param("orderStatus") OrderStatus orderStatus,
			@Param("paymentBefore") OffsetDateTime paymentBefore, @Param("orderBefore") OffsetDateTime orderBefore);
}
