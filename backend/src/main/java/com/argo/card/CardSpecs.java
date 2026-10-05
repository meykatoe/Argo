package com.argo.card;

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
				String like = "%" + q.keyword().trim().toLowerCase() + "%";
				ps.add(cb.or(
						cb.like(cb.lower(root.get("cardName")), like),
						cb.like(cb.lower(root.get("cardSetId")), like)));
			}
			if (has(q.setId())) {
				ps.add(cb.equal(root.get("setId"), q.setId()));
			}
			if (has(q.category())) {
				Subquery<String> sub = query.subquery(String.class);
				var set = sub.from(CardSet.class);
				sub.select(set.get("setId")).where(cb.equal(set.get("category"), q.category()));
				ps.add(root.get("setId").in(sub));
			}
			if (has(q.color())) {
				ps.add(cb.like(cb.lower(root.get("cardColor")), "%" + q.color().toLowerCase() + "%"));
			}
			if (has(q.rarity())) {
				ps.add(cb.equal(root.get("rarity"), q.rarity()));
			}
			if (has(q.cardType())) {
				ps.add(cb.equal(root.get("cardType"), q.cardType()));
			}
			return cb.and(ps.toArray(new Predicate[0]));
		};
	}

	private static boolean has(String s) {
		return s != null && !s.isBlank();
	}
}
