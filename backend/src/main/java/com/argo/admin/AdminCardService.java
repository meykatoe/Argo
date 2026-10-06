package com.argo.admin;

import com.argo.card.Card;
import com.argo.card.CardQuery;
import com.argo.card.CardRepository;
import com.argo.card.CardSpecs;
import com.argo.common.PageResult;
import com.argo.i18n.CardTranslation;
import com.argo.i18n.CardTranslationRepository;
import com.argo.i18n.Locales;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class AdminCardService {

	private static final int MAX_SIZE = 100;

	private final CardRepository cards;
	private final CardTranslationRepository translations;
	private final BigDecimal saleRate;
	private final AuditLogService audit;

	public AdminCardService(CardRepository cards, CardTranslationRepository translations,
			AuditLogService audit, @Value("${argo.pricing.sale-rate}") BigDecimal saleRate) {
		this.cards = cards;
		this.translations = translations;
		this.audit = audit;
		this.saleRate = saleRate;
	}

	@Transactional(readOnly = true)
	public PageResult<AdminCardView> search(String keyword, String setId, boolean discounted,
			int page, int size) {
		if (page < 1 || size < 1 || size > MAX_SIZE) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "INVALID_PAGING");
		}
		CardQuery q = new CardQuery(keyword, setId, null, null, null, null, false, Locales.ZH_TW);
		var spec = CardSpecs.of(q);
		if (discounted) {
			spec = spec.and((root, cq, cb) -> cb.lessThan(root.<BigDecimal>get("extraDiscount"), BigDecimal.ONE));
		}
		Sort sort = Sort.by("cardSetId").and(Sort.by("id"));
		var result = cards.findAll(spec, PageRequest.of(page - 1, size, sort));
		Map<String, CardTranslation> trs = names(result.getContent().stream().map(Card::getCardSetId).toList());
		return PageResult.of(result, c -> AdminCardView.from(c, trs.get(c.getCardSetId()), saleRate));
	}

	public AdminCardView setExtraDiscount(StaffAccount actor, Long id, BigDecimal extra) {
		Card card = cards.findById(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "CARD_NOT_FOUND"));
		BigDecimal discountBefore = card.getExtraDiscount();
		BigDecimal priceBefore = card.getSalePrice();
		card.applyExtraDiscount(extra, saleRate);
		// 與修改同一交易寫入紀錄
		audit.record(actor, null, AuditAction.CARD_EXTRA_DISCOUNT_UPDATE, true, "CARD",
				String.valueOf(id), Map.of("cardSetId", card.getCardSetId(),
						"extraDiscountBefore", discountBefore.toPlainString(),
						"extraDiscountAfter", card.getExtraDiscount().toPlainString(),
						"salePriceBefore", priceBefore.toPlainString(),
						"salePriceAfter", card.getSalePrice().toPlainString(),
						"priceOverridden", card.isPriceOverridden()));
		return AdminCardView.from(card, names(java.util.List.of(card.getCardSetId())).get(card.getCardSetId()), saleRate);
	}

	private Map<String, CardTranslation> names(Collection<String> ids) {
		if (ids.isEmpty()) {
			return Map.of();
		}
		return translations.findByLocaleAndCardSetIdIn(Locales.ZH_TW, ids).stream()
				.collect(Collectors.toMap(CardTranslation::getCardSetId, Function.identity()));
	}
}
