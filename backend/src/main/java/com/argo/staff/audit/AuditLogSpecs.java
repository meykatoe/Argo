package com.argo.staff.audit;

import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

public final class AuditLogSpecs {

	private AuditLogSpecs() {
	}

	public static Specification<AuditLog> of(AuditLogQuery q) {
		return (root, query, cb) -> {
			List<Predicate> ps = new ArrayList<>();
			if (q.username() != null && !q.username().isBlank()) {
				// 轉義萬用字元後做不分大小寫包含比對
				String esc = q.username().trim().toLowerCase().replace("\\", "\\\\")
						.replace("%", "\\%").replace("_", "\\_");
				ps.add(cb.like(cb.lower(root.get("username")), "%" + esc + "%", '\\'));
			}
			if (q.action() != null) {
				ps.add(cb.equal(root.get("action"), q.action()));
			}
			if (q.success() != null) {
				ps.add(cb.equal(root.get("success"), q.success()));
			}
			if (q.targetType() != null && !q.targetType().isBlank()) {
				ps.add(cb.equal(root.get("targetType"), q.targetType().trim()));
			}
			if (q.targetId() != null && !q.targetId().isBlank()) {
				ps.add(cb.equal(root.get("targetId"), q.targetId().trim()));
			}
			if (q.from() != null) {
				ps.add(cb.greaterThanOrEqualTo(root.get("createdAt"), q.from()));
			}
			if (q.to() != null) {
				ps.add(cb.lessThan(root.get("createdAt"), q.to()));
			}
			return cb.and(ps.toArray(Predicate[]::new));
		};
	}
}
