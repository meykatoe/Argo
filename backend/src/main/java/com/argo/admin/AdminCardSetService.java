package com.argo.admin;

import com.argo.card.Card;
import com.argo.card.CardRepository;
import com.argo.card.CardSet;
import com.argo.card.CardSetRepository;
import com.argo.i18n.CardSetTranslation;
import com.argo.i18n.CardSetTranslationRepository;
import com.argo.i18n.Locales;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class AdminCardSetService {

	private final CardSetRepository sets;
	private final CardRepository cards;
	private final CardSetTranslationRepository translations;
	private final AuditLogService audit;
	private final BigDecimal saleRate;

	public AdminCardSetService(CardSetRepository sets, CardRepository cards,
			CardSetTranslationRepository translations, AuditLogService audit,
			@Value("${argo.pricing.sale-rate}") BigDecimal saleRate) {
		this.sets = sets;
		this.cards = cards;
		this.translations = translations;
		this.audit = audit;
		this.saleRate = saleRate;
	}

	@Transactional(readOnly = true)
	public List<AdminCardSetView> list() {
		Map<String, String> names = names();
		Map<String, Object[]> stats = stats();
		return sets.findAll(Sort.by("setId")).stream().map(s -> view(s, names, stats)).toList();
	}

	public AdminCardSetView setOnSale(StaffAccount actor, String setId, boolean onSale) {
		CardSet set = find(setId);
		boolean before = set.isOnSale();
		set.setOnSale(onSale);
		// 沒有變動就不記錄
		if (before != onSale) {
			audit.record(actor, null, AuditAction.CARD_SET_ON_SALE_UPDATE, true, "CARD_SET", setId,
					Map.of("onSaleBefore", before, "onSaleAfter", onSale));
		}
		return view(set, names(), stats());
	}

	// 整個系列套用同一個額外折扣，已手動定價的卡只記折扣、不改價
	public AdminCardSetView setExtraDiscount(StaffAccount actor, String setId, BigDecimal extra) {
		CardSet set = find(setId);
		List<Card> list = cards.findBySetIdOrderByCardSetIdAscIdAsc(setId);
		Map<String, String> before = new LinkedHashMap<>();
		int overridden = 0;
		for (Card c : list) {
			if (c.getExtraDiscount().compareTo(extra) != 0) {
				before.put(String.valueOf(c.getId()), c.getExtraDiscount().toPlainString());
			}
			if (c.isPriceOverridden()) {
				overridden++;
			}
			c.applyExtraDiscount(extra, saleRate);
		}
		// 與修改同一交易寫入紀錄，附上被改動的卡原折扣
		Map<String, Object> detail = new LinkedHashMap<>();
		detail.put("extraDiscount", extra.toPlainString());
		detail.put("cardCount", list.size());
		detail.put("changedCount", before.size());
		detail.put("overriddenCount", overridden);
		detail.put("previousDiscounts", before);
		audit.record(actor, null, AuditAction.CARD_SET_EXTRA_DISCOUNT_UPDATE, true, "CARD_SET", setId,
				detail);
		cards.flush();
		return view(set, names(), stats());
	}

	private CardSet find(String setId) {
		return sets.findById(setId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "SET_NOT_FOUND"));
	}

	private Map<String, String> names() {
		return translations.findByLocale(Locales.ZH_TW).stream()
				.collect(Collectors.toMap(CardSetTranslation::getSetId, CardSetTranslation::getSetName));
	}

	private Map<String, Object[]> stats() {
		return cards.setStats().stream()
				.collect(Collectors.toMap(r -> (String) r[0], Function.identity()));
	}

	private static AdminCardSetView view(CardSet s, Map<String, String> names, Map<String, Object[]> stats) {
		Object[] st = stats.get(s.getSetId());
		long count = st == null ? 0 : (Long) st[1];
		BigDecimal min = st == null ? BigDecimal.ONE : (BigDecimal) st[2];
		BigDecimal max = st == null ? BigDecimal.ONE : (BigDecimal) st[3];
		String local = names.get(s.getSetId());
		return new AdminCardSetView(s.getSetId(), local != null ? local : s.getSetName(),
				s.getSetName(), s.getCategory(), s.isOnSale(), count, min, max);
	}
}
