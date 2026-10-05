package com.argo.card;

import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CardSetRepository extends JpaRepository<CardSet, String> {

	List<CardSet> findByCategory(String category, Sort sort);
}
