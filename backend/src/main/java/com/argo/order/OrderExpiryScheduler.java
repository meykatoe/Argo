package com.argo.order;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "argo.order.expire-scheduled", havingValue = "true")
public class OrderExpiryScheduler {

	private final OrderService orders;
	private final PaymentReconciler reconciler;

	public OrderExpiryScheduler(OrderService orders, PaymentReconciler reconciler) {
		this.orders = orders;
		this.reconciler = reconciler;
	}

	// 每分鐘檢查一次
	@Scheduled(fixedDelay = 60_000, initialDelay = 60_000)
	public void run() {
		// 先對帳再取消
		reconciler.run();
		orders.expireOverdue();
	}
}
