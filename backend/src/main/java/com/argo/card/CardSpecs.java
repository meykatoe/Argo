package com.argo.card;

import com.argo.i18n.CardTranslation;
import com.argo.i18n.Locales;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Subquery;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

public final class CardSpecs {

	private CardSpecs() {
	}

	public static Specification<Card> of(CardQuery q) {
		return (root, query, cb) -> {
			List<Predicate> ps = new ArrayList<>();
			if (has(q.keyword())) {
				// 比對名稱與編號
				String like = "%" + q.keyword().trim().toLowerCase() + "%";
				Predicate hit = cb.or(
						cb.like(cb.lower(root.get("cardName")), like),
						cb.like(cb.lower(root.get("cardSetId")), like));
				if (!Locales.isDefault(q.lang())) {
					// 也比對翻譯名稱
					Subquery<Long> tr = query.subquery(Long.class);
					var t = tr.from(CardTranslation.class);
					tr.select(t.get("id")).where(
							cb.equal(t.get("cardSetId"), root.get("cardSetId")),
							cb.equal(t.get("locale"), q.lang()),
							cb.like(cb.lower(t.get("cardName")), like));
					hit = cb.or(hit, cb.exists(tr));
				}
				ps.add(hit);
			}
			if (has(q.setId())) {
				ps.add(cb.equal(root.get("setId"), q.setId()));
			}
			if (has(q.category())) {
				// 類別在系列表
				Subquery<String> sub = query.subquery(String.class);
				var set = sub.from(CardSet.class);
				sub.select(set.get("setId")).where(cb.equal(set.get("category"), q.category()));
				ps.add(root.get("setId").in(sub));
			}
			if (has(q.color())) {
				// 多色卡用包含比對
				ps.add(cb.like(cb.lower(root.get("cardColor")), "%" + q.color().toLowerCase() + "%"));
			}
			if (has(q.rarity())) {
				ps.add(cb.equal(root.get("rarity"), q.rarity()));
			}
			if (has(q.cardType())) {
				ps.add(cb.equal(root.get("cardType"), q.cardType()));
			}
			if (q.inStock()) {
				// 有庫存且已定價
				ps.add(cb.greaterThan(root.<Integer>get("stock"), 0));
				ps.add(cb.greaterThan(root.<java.math.BigDecimal>get("salePrice"), java.math.BigDecimal.ZERO));
			}
			return cb.and(ps.toArray(new Predicate[0]));
		};
	}

	private static boolean has(String s) {
		return s != null && !s.isBlank();
	}
}
