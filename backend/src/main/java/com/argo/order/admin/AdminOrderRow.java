package com.argo.order.admin;

import com.argo.order.OrderStatus;
import com.argo.order.ShopOrder;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record AdminOrderRow(String orderNo, OrderStatus status, String currency, BigDecimal total,
		String customerName, String customerEmail, int itemCount, OffsetDateTime createdAt,
		OffsetDateTime paidAt) {

	static AdminOrderRow from(ShopOrder o) {
		int count = o.getItems().stream().mapToInt(i -> i.getQuantity()).sum();
		return new AdminOrderRow(o.getOrderNo(), o.getStatus(), o.getCurrency(), o.getTotal(),
				o.getCustomerName(), o.getCustomerEmail(), count, o.getCreatedAt(), o.getPaidAt());
	}
}
