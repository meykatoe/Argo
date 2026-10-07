package com.argo.card.i18n;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CardTranslationRepository extends JpaRepository<CardTranslation, Long> {

	List<CardTranslation> findByLocale(String locale);

	List<CardTranslation> findByLocaleAndCardSetIdIn(String locale, Collection<String> cardSetIds);
}
