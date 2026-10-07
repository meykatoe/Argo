package com.argo.card;

import com.argo.common.ApiException;
import com.argo.common.ErrorCode;
import com.argo.common.PageResult;
import com.argo.card.i18n.CardSetTranslation;
import com.argo.card.i18n.CardSetTranslationRepository;
import com.argo.card.i18n.CardTranslation;
import com.argo.card.i18n.CardTranslationRepository;
import com.argo.card.i18n.Locales;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CardService {

	// 排序欄位白名單
	private static final Set<String> SORTS = Set.of("cardSetId", "cardName", "marketPrice", "salePrice");
	private static final int MAX_SIZE = 100;
	private static final int MAX_BATCH = 50;

	private final CardRepository cards;
	private final CardSetRepository sets;
	private final CardTranslationRepository translations;
	private final CardSetTranslationRepository setTrs;
	private final BigDecimal saleRate;

	public CardService(CardRepository cards, CardSetRepository sets,
			CardTranslationRepository translations, CardSetTranslationRepository setTrs,
			@Value("${argo.pricing.sale-rate}") BigDecimal saleRate) {
		this.saleRate = saleRate;
		this.cards = cards;
		this.sets = sets;
		this.translations = translations;
		this.setTrs = setTrs;
	}

	public PageResult<CardSummary> search(CardQuery query, int page, int size,
			String sortBy, boolean desc) {
		// 頁碼從一起算
		if (page < 1 || size < 1 || size > MAX_SIZE) {
			throw new ApiException(ErrorCode.INVALID_PAGING);
		}
		if (!SORTS.contains(sortBy)) {
			throw new ApiException(ErrorCode.INVALID_SORT);
		}
		// 加主鍵穩定分頁
		Sort sort = Sort.by(desc ? Sort.Direction.DESC : Sort.Direction.ASC, sortBy)
				.and(Sort.by("id"));
		CardQuery q = withLocale(query);
		var result = cards.findAll(CardSpecs.of(q), PageRequest.of(page - 1, size, sort));
		Map<String, CardTranslation> trs = translations(q.lang(),
				result.getContent().stream().map(Card::getCardSetId).toList());
		Map<String, Boolean> shelf = shelf(result.getContent());
		return PageResult.of(result, c -> CardSummary.from(c, trs.get(c.getCardSetId()), saleRate,
				shelf.getOrDefault(c.getSetId(), false)));
	}

	// 依編號批次取卡，保留請求順序
	public List<CardSummary> getAll(List<Long> ids, String lang) {
		if (ids.isEmpty() || ids.size() > MAX_BATCH) {
			throw new ApiException(ErrorCode.INVALID_IDS);
		}
		String locale = Locales.normalize(lang);
		Map<Long, Card> found = cards.findAllById(ids).stream()
				.collect(Collectors.toMap(Card::getId, Function.identity()));
		Map<String, CardTranslation> trs = translations(locale,
				found.values().stream().map(Card::getCardSetId).distinct().toList());
		Map<String, Boolean> shelf = shelf(found.values());
		return ids.stream().distinct().map(found::get).filter(java.util.Objects::nonNull)
				.map(c -> CardSummary.from(c, trs.get(c.getCardSetId()), saleRate,
						shelf.getOrDefault(c.getSetId(), false)))
				.toList();
	}

	public CardDetail get(Long id, String lang) {
		String locale = Locales.normalize(lang);
		Card card = cards.findById(id)
				.orElseThrow(() -> new ApiException(ErrorCode.CARD_NOT_FOUND));
		String setName = setTranslations(locale).get(card.getSetId());
		if (setName == null) {
			setName = sets.findById(card.getSetId()).map(CardSet::getSetName).orElse(null);
		}
		CardTranslation tr = translations(locale, List.of(card.getCardSetId())).get(card.getCardSetId());
		boolean onSale = sets.findById(card.getSetId()).map(CardSet::isOnSale).orElse(false);
		return CardDetail.from(card, setName, tr, saleRate, onSale);
	}

	public List<CardSetDto> listSets(String category, String lang) {
		Map<String, String> names = setTranslations(Locales.normalize(lang));
		List<CardSet> all = category == null || category.isBlank()
				? sets.findAll(Sort.by("setId"))
				: sets.findByCategory(category, Sort.by("setId"));
		return all.stream().map(s -> CardSetDto.from(s, names.get(s.getSetId()))).toList();
	}

	// 系列是否上架，找不到系列視為未上架
	private Map<String, Boolean> shelf(Collection<Card> list) {
		List<String> ids = list.stream().map(Card::getSetId).distinct().toList();
		return sets.findAllById(ids).stream()
				.collect(Collectors.toMap(CardSet::getSetId, CardSet::isOnSale));
	}

	private CardQuery withLocale(CardQuery q) {
		return new CardQuery(q.keyword(), q.setId(), q.category(), q.color(), q.rarity(),
				q.cardType(), q.inStock(), Locales.normalize(q.lang()));
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
