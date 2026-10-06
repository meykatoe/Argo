package com.argo.admin;

import com.argo.card.Card;
import com.argo.card.CardQuery;
import com.argo.card.CardRepository;
import com.argo.card.CardSpecs;
import com.argo.common.PageResult;
import java.math.BigDecimal;
import java.util.Map;
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
	private final BigDecimal saleRate;
	private final AuditLogService audit;

	public AdminCardService(CardRepository cards, AuditLogService audit,
			@Value("${argo.pricing.sale-rate}") BigDecimal saleRate) {
		this.cards = cards;
		this.audit = audit;
		this.saleRate = saleRate;
	}

	@Transactional(readOnly = true)
	public PageResult<AdminCardView> search(String keyword, String setId, boolean discounted,
			int page, int size) {
		if (page < 1 || size < 1 || size > MAX_SIZE) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "INVALID_PAGING");
		}
		CardQuery q = new CardQuery(keyword, setId, null, null, null, null, false, "en");
		var spec = CardSpecs.of(q);
		if (discounted) {
			spec = spec.and((root, cq, cb) -> cb.lessThan(root.<BigDecimal>get("extraDiscount"), BigDecimal.ONE));
		}
		Sort sort = Sort.by("cardSetId").and(Sort.by("id"));
		return PageResult.of(cards.findAll(spec, PageRequest.of(page - 1, size, sort)),
				c -> AdminCardView.from(c, saleRate));
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
		return AdminCardView.from(card, saleRate);
	}
}
