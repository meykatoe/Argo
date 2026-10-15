package com.argo.order.admin;

import com.argo.order.Payment;
import com.argo.order.ShopOrder;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record AdminRefundRow(Long id, String orderNo, String orderStatus, String tradeNo, BigDecimal amount,
		String currency, String refundStatus, OffsetDateTime paidAt, OffsetDateTime refundedAt) {

	static AdminRefundRow of(Payment p, ShopOrder o) {
		return new AdminRefundRow(p.getId(), o == null ? null : o.getOrderNo(),
				o == null ? null : o.getStatus().name(), p.getTradeNo(), p.getAmount(), p.getCurrency(),
				p.getRefundStatus(), p.getUpdatedAt(), p.getRefundedAt());
	}
}
