package com.argo.order;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

	Optional<Payment> findFirstByOrderIdOrderByIdDesc(Long orderId);
}
