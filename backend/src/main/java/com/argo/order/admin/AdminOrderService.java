package com.argo.order.admin;

import com.argo.common.ApiException;
import com.argo.common.ErrorCode;
import com.argo.common.PageResult;
import com.argo.order.OrderService;
import com.argo.order.OrderStatus;
import com.argo.order.PaymentRepository;
import com.argo.order.ShopOrder;
import com.argo.order.ShopOrderRepository;
import com.argo.staff.audit.AuditAction;
import com.argo.staff.audit.AuditLogService;
import com.argo.staff.auth.StaffAccount;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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

	private final OrderService orderService;
	private final AuditLogService audit;

	public AdminOrderService(ShopOrderRepository orders, PaymentRepository payments,
			OrderService orderService, AuditLogService audit) {
		this.orders = orders;
		this.payments = payments;
		this.orderService = orderService;
		this.audit = audit;
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

	// 已付款才能出貨
	@Transactional
	public AdminOrderView ship(StaffAccount actor, String orderNo, String trackingNo) {
		ShopOrder o = lock(orderNo);
		require(o, OrderStatus.PAID);
		String tracking = blankToNull(trackingNo);
		o.markShipped(tracking);
		Map<String, Object> detail = new LinkedHashMap<>();
		detail.put("hasTracking", tracking == null ? 0 : 1);
		audit.record(actor, null, AuditAction.ORDER_SHIPPED, true, "ORDER", orderNo, detail);
		return get(orderNo);
	}

	@Transactional
	public AdminOrderView complete(StaffAccount actor, String orderNo) {
		ShopOrder o = lock(orderNo);
		require(o, OrderStatus.SHIPPED);
		o.markCompleted();
		audit.record(actor, null, AuditAction.ORDER_COMPLETED, true, "ORDER", orderNo, null);
		return get(orderNo);
	}

	// 待付款或已付款可取消，並退回庫存
	@Transactional
	public AdminOrderView cancel(StaffAccount actor, String orderNo) {
		ShopOrder o = lock(orderNo);
		require(o, OrderStatus.PENDING_PAYMENT, OrderStatus.PAID);
		OrderStatus before = o.getStatus();
		orderService.cancelInternal(o, "STAFF");
		audit.record(actor, null, AuditAction.ORDER_CANCELLED, true, "ORDER", orderNo,
				Map.of("statusBefore", before.name()));
		return get(orderNo);
	}

	@Transactional
	public AdminOrderView setNote(StaffAccount actor, String orderNo, String note) {
		ShopOrder o = lock(orderNo);
		String text = blankToNull(note);
		o.setStaffNote(text);
		// 內容不寫入稽核
		audit.record(actor, null, AuditAction.ORDER_NOTE_UPDATED, true, "ORDER", orderNo,
				Map.of("length", text == null ? 0 : text.length()));
		return get(orderNo);
	}

	private ShopOrder lock(String orderNo) {
		return orders.findByOrderNoForUpdate(orderNo)
				.orElseThrow(() -> new ApiException(ErrorCode.ORDER_NOT_FOUND));
	}

	private static void require(ShopOrder o, OrderStatus... allowed) {
		for (OrderStatus s : allowed) {
			if (o.getStatus() == s) {
				return;
			}
		}
		throw new ApiException(ErrorCode.ORDER_STATE_CONFLICT, Map.of("status", o.getStatus().name()));
	}

	private static String blankToNull(String s) {
		return s == null || s.isBlank() ? null : s.trim();
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
