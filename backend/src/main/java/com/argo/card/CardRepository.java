package com.argo.card;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CardRepository extends JpaRepository<Card, Long> {

	List<Card> findBySourceKeyIn(Collection<String> keys);
}
