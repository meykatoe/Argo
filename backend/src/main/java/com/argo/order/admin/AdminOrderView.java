package com.argo.order.admin;

import com.argo.order.OrderStatus;
import com.argo.order.OrderView;
import com.argo.order.Payment;
import com.argo.order.ShopOrder;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public record AdminOrderView(String orderNo, OrderStatus status, String currency, BigDecimal subtotal,
		BigDecimal shippingFee, BigDecimal total, Long customerId, String customerName,
		String customerEmail, String customerPhone, String recipientName, String recipientPhone,
		String postalCode, String city, String address, String cancelReason, OffsetDateTime createdAt,
		OffsetDateTime paidAt, OffsetDateTime cancelledAt, OffsetDateTime updatedAt,
		List<OrderView.ItemView> items, OrderView.PaymentView payment) {

	static AdminOrderView from(ShopOrder o, Payment lastPayment) {
		return new AdminOrderView(o.getOrderNo(), o.getStatus(), o.getCurrency(), o.getSubtotal(),
				o.getShippingFee(), o.getTotal(), o.getCustomerId(), o.getCustomerName(),
				o.getCustomerEmail(), o.getCustomerPhone(), o.getRecipientName(), o.getRecipientPhone(),
				o.getPostalCode(), o.getCity(), o.getAddress(), o.getCancelReason(), o.getCreatedAt(),
				o.getPaidAt(), o.getCancelledAt(), o.getUpdatedAt(),
				o.getItems().stream().map(OrderView.ItemView::from).toList(),
				OrderView.PaymentView.from(lastPayment));
	}
}
