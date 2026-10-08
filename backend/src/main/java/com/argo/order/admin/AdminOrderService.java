package com.argo.order.admin;

import com.argo.common.ApiException;
import com.argo.common.ErrorCode;
import com.argo.common.PageResult;
import com.argo.order.OrderStatus;
import com.argo.order.PaymentRepository;
import com.argo.order.ShopOrder;
import com.argo.order.ShopOrderRepository;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminOrderService {

	private static final int MAX_PAGE_SIZE = 50;

	private final ShopOrderRepository orders;
	private final PaymentRepository payments;

	public AdminOrderService(ShopOrderRepository orders, PaymentRepository payments) {
		this.orders = orders;
		this.payments = payments;
	}

	@Transactional(readOnly = true)
	public PageResult<AdminOrderRow> search(String keyword, OrderStatus status, int page, int size) {
		var pageable = PageRequest.of(Math.max(page, 1) - 1, Math.min(Math.max(size, 1), MAX_PAGE_SIZE),
				Sort.by(Sort.Direction.DESC, "createdAt", "id"));
		return PageResult.of(orders.findAll(spec(keyword, status), pageable), AdminOrderRow::from);
	}

	@Transactional(readOnly = true)
	public AdminOrderView get(String orderNo) {
		ShopOrder o = orders.findByOrderNo(orderNo)
				.orElseThrow(() -> new ApiException(ErrorCode.ORDER_NOT_FOUND));
		return AdminOrderView.from(o, payments.findFirstByOrderIdOrderByIdDesc(o.getId()).orElse(null));
	}

	private static Specification<ShopOrder> spec(String keyword, OrderStatus status) {
		return (root, query, cb) -> {
			List<Predicate> ps = new ArrayList<>();
			if (status != null) {
				ps.add(cb.equal(root.get("status"), status));
			}
			if (keyword != null && !keyword.isBlank()) {
				// 比對編號、信箱與姓名
				String like = "%" + keyword.trim().toLowerCase() + "%";
				ps.add(cb.or(cb.like(cb.lower(root.get("orderNo")), like),
						cb.like(cb.lower(root.get("customerEmail")), like),
						cb.like(cb.lower(root.get("customerName")), like),
						cb.like(cb.lower(root.get("recipientName")), like)));
			}
			return cb.and(ps.toArray(new Predicate[0]));
		};
	}
}
