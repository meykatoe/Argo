package com.argo.card;

import com.argo.common.PageResult;
import com.argo.i18n.CardSetTranslation;
import com.argo.i18n.CardSetTranslationRepository;
import com.argo.i18n.CardTranslation;
import com.argo.i18n.CardTranslationRepository;
import com.argo.i18n.Locales;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
public class CardService {

	// 排序欄位白名單
	private static final Set<String> SORTS = Set.of("cardSetId", "cardName", "marketPrice");
	private static final int MAX_SIZE = 100;

	private final CardRepository cards;
	private final CardSetRepository sets;
	private final CardTranslationRepository translations;
	private final CardSetTranslationRepository setTrs;

	public CardService(CardRepository cards, CardSetRepository sets,
			CardTranslationRepository translations, CardSetTranslationRepository setTrs) {
		this.cards = cards;
		this.sets = sets;
		this.translations = translations;
		this.setTrs = setTrs;
	}

	public PageResult<CardSummary> search(CardQuery query, int page, int size,
			String sortBy, boolean desc) {
		// 頁碼從一起算
		if (page < 1 || size < 1 || size > MAX_SIZE) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "INVALID_PAGING");
		}
		if (!SORTS.contains(sortBy)) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "INVALID_SORT");
		}
		// 加主鍵穩定分頁
		Sort sort = Sort.by(desc ? Sort.Direction.DESC : Sort.Direction.ASC, sortBy)
				.and(Sort.by("id"));
		CardQuery q = withLocale(query);
		var result = cards.findAll(CardSpecs.of(q), PageRequest.of(page - 1, size, sort));
		Map<String, CardTranslation> trs = translations(q.lang(),
				result.getContent().stream().map(Card::getCardSetId).toList());
		return PageResult.of(result, c -> CardSummary.from(c, trs.get(c.getCardSetId())));
	}

	public CardDetail get(Long id, String lang) {
		String locale = Locales.normalize(lang);
		Card card = cards.findById(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "CARD_NOT_FOUND"));
		String setName = setTranslations(locale).get(card.getSetId());
		if (setName == null) {
			setName = sets.findById(card.getSetId()).map(CardSet::getSetName).orElse(null);
		}
		CardTranslation tr = translations(locale, List.of(card.getCardSetId())).get(card.getCardSetId());
		return CardDetail.from(card, setName, tr);
	}

	public List<CardSetDto> listSets(String category, String lang) {
		Map<String, String> names = setTranslations(Locales.normalize(lang));
		List<CardSet> all = category == null || category.isBlank()
				? sets.findAll(Sort.by("setId"))
				: sets.findByCategory(category, Sort.by("setId"));
		return all.stream().map(s -> CardSetDto.from(s, names.get(s.getSetId()))).toList();
	}

	private CardQuery withLocale(CardQuery q) {
		return new CardQuery(q.keyword(), q.setId(), q.category(), q.color(), q.rarity(),
				q.cardType(), Locales.normalize(q.lang()));
	}

	// 預設語言不需查翻譯
	private Map<String, CardTranslation> translations(String locale, Collection<String> ids) {
		if (Locales.isDefault(locale) || ids.isEmpty()) {
			return Map.of();
		}
		return translations.findByLocaleAndCardSetIdIn(locale, ids).stream()
				.collect(Collectors.toMap(CardTranslation::getCardSetId, Function.identity()));
	}

	private Map<String, String> setTranslations(String locale) {
		if (Locales.isDefault(locale)) {
			return Map.of();
		}
		return setTrs.findByLocale(locale).stream()
				.collect(Collectors.toMap(CardSetTranslation::getSetId, CardSetTranslation::getSetName));
	}
}
