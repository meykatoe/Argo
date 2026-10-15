package com.argo.order.admin;

import com.argo.common.ApiException;
import com.argo.common.ErrorCode;
import com.argo.common.PageResult;
import com.argo.order.Payment;
import com.argo.order.PaymentRepository;
import com.argo.order.ShopOrder;
import com.argo.order.ShopOrderRepository;
import com.argo.staff.audit.AuditAction;
import com.argo.staff.audit.AuditLogService;
import com.argo.staff.auth.StaffAccount;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminRefundService {

	private static final int MAX_PAGE_SIZE = 50;

	private final PaymentRepository payments;
	private final ShopOrderRepository orders;
	private final AuditLogService audit;

	public AdminRefundService(PaymentRepository payments, ShopOrderRepository orders, AuditLogService audit) {
		this.payments = payments;
		this.orders = orders;
		this.audit = audit;
	}

	@Transactional(readOnly = true)
	public PageResult<AdminRefundRow> list(String status, int page, int size) {
		if (!Payment.REFUND_REQUIRED.equals(status) && !Payment.REFUND_REFUNDED.equals(status)) {
			throw new ApiException(ErrorCode.VALIDATION_ERROR);
		}
		var pageable = PageRequest.of(Math.max(page, 1) - 1, Math.min(Math.max(size, 1), MAX_PAGE_SIZE),
				Sort.by(Sort.Direction.DESC, "updatedAt", "id"));
		var found = payments.findByRefundStatus(status, pageable);
		Map<Long, ShopOrder> byId = orders
				.findAllById(found.getContent().stream().map(Payment::getOrderId).toList()).stream()
				.collect(Collectors.toMap(ShopOrder::getId, Function.identity()));
		return PageResult.of(found, p -> AdminRefundRow.of(p, byId.get(p.getOrderId())));
	}

	// 重複標記不會再寫稽核
	@Transactional
	public AdminRefundRow markRefunded(StaffAccount actor, Long id) {
		Payment p = payments.findByIdForUpdate(id).orElseThrow(() -> new ApiException(ErrorCode.REFUND_NOT_FOUND));
		if (Payment.REFUND_NONE.equals(p.getRefundStatus())) {
			throw new ApiException(ErrorCode.REFUND_NOT_REQUIRED);
		}
		ShopOrder o = orders.findById(p.getOrderId()).orElse(null);
		if (Payment.REFUND_REQUIRED.equals(p.getRefundStatus())) {
			p.markRefunded(actor.getId());
			payments.flush();
			audit.record(actor, null, AuditAction.REFUND_MARKED, true, "PAYMENT", String.valueOf(p.getId()),
					Map.of("orderNo", o == null ? "" : o.getOrderNo(), "tradeNo", p.getTradeNo(),
							"amount", p.getAmount().toPlainString(), "currency", p.getCurrency()));
		}
		return AdminRefundRow.of(p, o);
	}
}
