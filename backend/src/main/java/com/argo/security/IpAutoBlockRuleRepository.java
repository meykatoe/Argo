package com.argo.security;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IpAutoBlockRuleRepository extends JpaRepository<IpAutoBlockRule, Long> {

	Optional<IpAutoBlockRule> findByMetric(AutoBlockMetric metric);
}
