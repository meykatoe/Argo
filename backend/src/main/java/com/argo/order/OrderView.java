package com.argo.order;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public record OrderView(String orderNo, OrderStatus status, String currency, BigDecimal subtotal,
		BigDecimal shippingFee, BigDecimal total, OffsetDateTime createdAt, OffsetDateTime expiresAt,
		OffsetDateTime paidAt, String cancelReason, String customerName, String customerEmail,
		String recipientName, String recipientPhone, String postalCode, String city, String address,
		List<ItemView> items, PaymentView payment) {

	public record ItemView(Long cardId, String cardSetId, String cardName, String cardNameEn,
			String imageUrl, BigDecimal unitPrice, int quantity, BigDecimal subtotal) {

		static ItemView from(OrderItem i) {
			return new ItemView(i.getCardId(), i.getCardSetId(), i.getCardName(), i.getCardNameEn(),
					i.getImageUrl(), i.getUnitPrice(), i.getQuantity(), i.getSubtotal());
		}
	}

	public record PaymentView(String status, String cardLast4, String failureCode,
			OffsetDateTime createdAt) {

		static PaymentView from(Payment p) {
			return p == null ? null
					: new PaymentView(p.getStatus(), p.getCardLast4(), p.getFailureCode(), p.getCreatedAt());
		}
	}

	public static OrderView from(ShopOrder o, Payment lastPayment, long expireMinutes) {
		return new OrderView(o.getOrderNo(), o.getStatus(), o.getCurrency(), o.getSubtotal(),
				o.getShippingFee(), o.getTotal(), o.getCreatedAt(), o.getCreatedAt().plusMinutes(expireMinutes),
				o.getPaidAt(), o.getCancelReason(), o.getCustomerName(), o.getCustomerEmail(),
				o.getRecipientName(), o.getRecipientPhone(), o.getPostalCode(), o.getCity(), o.getAddress(),
				o.getItems().stream().map(ItemView::from).toList(), PaymentView.from(lastPayment));
	}
}
