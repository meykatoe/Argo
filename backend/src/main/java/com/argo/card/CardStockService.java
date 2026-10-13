package com.argo.card;

import com.argo.card.i18n.CardTranslation;
import com.argo.card.i18n.CardTranslationRepository;
import com.argo.card.i18n.Locales;
import com.argo.common.ApiException;
import com.argo.common.ErrorCode;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 供訂單預留與歸還庫存
@Service
public class CardStockService {

	// 預留成功的一列
	public record Reserved(Long cardId, String cardSetId, String name, String originalName,
			String imageUrl, BigDecimal unitPrice, int quantity) {
	}

	private final CardRepository cards;
	private final CardSetRepository sets;
	private final CardTranslationRepository translations;

	public CardStockService(CardRepository cards, CardSetRepository sets,
			CardTranslationRepository translations) {
		this.cards = cards;
		this.sets = sets;
		this.translations = translations;
	}

	// 依編號順序扣庫存，鍵須已排序以避免死鎖
	@Transactional
	public List<Reserved> reserve(Map<Long, Integer> wanted, String locale) {
		Map<Long, Card> found = cards.findAllById(wanted.keySet()).stream()
				.collect(Collectors.toMap(Card::getId, Function.identity()));
		Map<String, CardTranslation> trs = translationsFor(locale, found.values());
		List<Reserved> lines = new ArrayList<>();
		for (Map.Entry<Long, Integer> e : wanted.entrySet()) {
			Card card = found.get(e.getKey());
			Map<String, String> detail = Map.of("cardId", String.valueOf(e.getKey()));
			if (card == null) {
				throw new ApiException(ErrorCode.ITEM_NOT_FOUND, detail);
			}
			// 系列下架的卡不可購買
			boolean onSale = sets.findById(card.getSetId()).map(CardSet::isOnSale).orElse(false);
			if (!onSale || card.getStock() <= 0 || card.getSalePrice().signum() <= 0) {
				throw new ApiException(ErrorCode.ITEM_UNAVAILABLE, detail);
			}
			// 單一語句扣庫存，擋住同時下單
			if (cards.decrementStock(card.getId(), e.getValue()) == 0) {
				throw new ApiException(ErrorCode.INSUFFICIENT_STOCK, detail);
			}
			CardTranslation tr = trs.get(card.getCardSetId());
			lines.add(new Reserved(card.getId(), card.getCardSetId(),
					tr != null ? tr.getCardName() : card.getCardName(), card.getCardName(),
					card.getImageUrl(), card.getSalePrice(), e.getValue()));
		}
		return lines;
	}

	@Transactional
	public void release(Long cardId, int quantity) {
		cards.incrementStock(cardId, quantity);
	}

	private Map<String, CardTranslation> translationsFor(String locale, Collection<Card> list) {
		if (Locales.isDefault(locale) || list.isEmpty()) {
			return Map.of();
		}
		List<String> ids = list.stream().map(Card::getCardSetId).distinct().toList();
		return translations.findByLocaleAndCardSetIdIn(locale, ids).stream()
				.collect(Collectors.toMap(CardTranslation::getCardSetId, Function.identity()));
	}
}
