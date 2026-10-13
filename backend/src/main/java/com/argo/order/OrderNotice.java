package com.argo.order;

import java.math.BigDecimal;
import java.util.List;

// 寄信用的訂單快照
public record OrderNotice(Kind kind, String orderNo, String email, String name, String lang,
		String currency, BigDecimal shippingFee, BigDecimal total, List<Line> items,
		String trackingNo, String cancelReason) {

	public enum Kind {
		CREATED, PAID, SHIPPED, CANCELLED
	}

	public record Line(String name, int quantity, BigDecimal subtotal) {
	}

	public static OrderNotice of(Kind kind, ShopOrder o) {
		boolean zh = "zh-TW".equals(o.getLang());
		List<Line> lines = o.getItems().stream()
				.map(i -> new Line(zh ? i.getCardName() : i.getCardNameEn(), i.getQuantity(), i.getSubtotal()))
				.toList();
		return new OrderNotice(kind, o.getOrderNo(), o.getCustomerEmail(), o.getCustomerName(), o.getLang(),
				o.getCurrency(), o.getShippingFee(), o.getTotal(), lines, o.getTrackingNo(), o.getCancelReason());
	}
}
