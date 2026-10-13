package com.argo.order;

import com.argo.common.mail.Mailer;
import java.math.BigDecimal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

// 提交後才寄信
@Component
class OrderNotifier {

	private static final Logger log = LoggerFactory.getLogger(OrderNotifier.class);

	private final Mailer mailer;
	private final String siteUrl;
	private final long expireMinutes;

	OrderNotifier(Mailer mailer, @Value("${argo.site-url}") String siteUrl,
			@Value("${argo.order.expire-minutes}") long expireMinutes) {
		this.mailer = mailer;
		this.siteUrl = siteUrl;
		this.expireMinutes = expireMinutes;
	}

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	void onNotice(OrderNotice n) {
		try {
			boolean zh = "zh-TW".equals(n.lang());
			mailer.send(n.email(), subject(n, zh), body(n, zh));
		}
		catch (RuntimeException e) {
			// 寄信失敗不影響訂單
			log.warn("Order mail failed order={}: {}", n.orderNo(), e.getMessage());
		}
	}

	String subject(OrderNotice n, boolean zh) {
		String no = n.orderNo();
		return switch (n.kind()) {
			case CREATED -> zh ? "Argo 訂單 " + no + " 已成立" : "Argo order " + no + " received";
			case PAID -> zh ? "Argo 訂單 " + no + " 已收到付款" : "Argo order " + no + " payment received";
			case SHIPPED -> zh ? "Argo 訂單 " + no + " 已出貨" : "Argo order " + no + " has shipped";
			case CANCELLED -> zh ? "Argo 訂單 " + no + " 已取消" : "Argo order " + no + " cancelled";
		};
	}

	String body(OrderNotice n, boolean zh) {
		StringBuilder sb = new StringBuilder();
		sb.append(zh ? n.name() + " 您好，\n\n" : "Hello " + n.name() + ",\n\n");
		sb.append(intro(n, zh)).append("\n\n");
		sb.append(zh ? "訂單編號：" : "Order number: ").append(n.orderNo()).append('\n');
		for (OrderNotice.Line l : n.items()) {
			sb.append("- ").append(l.name()).append(" x ").append(l.quantity()).append("  ")
					.append(money(n, l.subtotal())).append('\n');
		}
		sb.append(zh ? "運費：" : "Shipping: ").append(money(n, n.shippingFee())).append('\n');
		sb.append(zh ? "總計：" : "Total: ").append(money(n, n.total())).append("\n\n");
		sb.append(zh ? "查詢訂單（需訂單編號與下單 Email）：" : "Look up your order (order number and email needed): ")
				.append(siteUrl).append("/orders");
		return sb.toString();
	}

	private String intro(OrderNotice n, boolean zh) {
		return switch (n.kind()) {
			case CREATED -> zh ? "我們已收到您的訂單，請在 " + expireMinutes + " 分鐘內完成付款，逾時將自動取消。"
					: "We received your order. Please pay within " + expireMinutes
							+ " minutes or it will be cancelled automatically.";
			case PAID -> zh ? "已收到您的付款，我們會盡快為您安排出貨。"
					: "We received your payment and will prepare your shipment soon.";
			case SHIPPED -> shipped(n, zh);
			case CANCELLED -> cancelled(n, zh);
		};
	}

	private String shipped(OrderNotice n, boolean zh) {
		String base = zh ? "您的訂單已出貨。" : "Your order has shipped.";
		if (n.trackingNo() == null) {
			return base;
		}
		return base + (zh ? "\n物流單號：" : "\nTracking number: ") + n.trackingNo();
	}

	private String cancelled(OrderNotice n, boolean zh) {
		String why = switch (String.valueOf(n.cancelReason())) {
			case "CUSTOMER" -> zh ? "依您的要求取消" : "cancelled at your request";
			case "EXPIRED" -> zh ? "逾時未付款" : "payment was not made in time";
			case "STAFF" -> zh ? "由店家取消" : "cancelled by the shop";
			default -> zh ? "已取消" : "cancelled";
		};
		return zh ? "您的訂單已取消（" + why + "）。" : "Your order has been cancelled (" + why + ").";
	}

	private static String money(OrderNotice n, BigDecimal v) {
		return n.currency() + " " + v.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
	}
}
