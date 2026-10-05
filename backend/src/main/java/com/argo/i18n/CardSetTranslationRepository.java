package com.argo.i18n;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CardSetTranslationRepository
		extends JpaRepository<CardSetTranslation, CardSetTranslation.Key> {

	List<CardSetTranslation> findByLocale(String locale);
}
