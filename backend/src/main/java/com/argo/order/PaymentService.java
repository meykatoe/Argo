package com.argo.order;

import com.argo.common.ApiException;
import com.argo.common.ErrorCode;
import java.math.BigDecimal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentService {

	private static final Logger log = LoggerFactory.getLogger(PaymentService.class);
	static final String ECPAY = "ECPAY_CREDIT";
	static final String FAKE = "FAKE";

	// 回報結果：是否接受，以及對應的訂單編號
	public record Outcome(boolean accepted, String orderNo) {
	}

	private final OrderService orderService;
	private final ShopOrderRepository orders;
	private final PaymentRepository payments;
	private final EcpayCheckout ecpay;
	private final ApplicationEventPublisher events;
	private final String mode;
	private final String apiUrl;
	private final String siteUrl;

	public PaymentService(OrderService orderService, ShopOrderRepository orders, PaymentRepository payments,
			EcpayCheckout ecpay, ApplicationEventPublisher events,
			@Value("${argo.payment.mode}") String mode, @Value("${argo.payment.api-url}") String apiUrl,
			@Value("${argo.site-url}") String siteUrl) {
		this.orderService = orderService;
		this.orders = orders;
		this.payments = payments;
		this.ecpay = ecpay;
		this.events = events;
		this.mode = mode;
		this.apiUrl = apiUrl;
		this.siteUrl = siteUrl;
	}

	public boolean isFake() {
		return "fake".equalsIgnoreCase(mode);
	}

	// 每次呼叫建立一筆待付款，交易編號不重複
	@Transactional(noRollbackFor = ApiException.class)
	public PayStart start(String orderNo, String email) {
		ShopOrder order = orderService.findForUpdate(orderNo, email);
		if (order.getStatus() != OrderStatus.PENDING_PAYMENT || order.getTotalTwd() == null) {
			throw new ApiException(ErrorCode.ORDER_NOT_PAYABLE);
		}
		if (orderService.isExpired(order)) {
			orderService.cancelInternal(order, "EXPIRED");
			throw new ApiException(ErrorCode.ORDER_EXPIRED);
		}

		String tradeNo = order.getOrderNo().replace("-", "")
				+ String.format("%02d", payments.countByOrderId(order.getId()) + 1);
		payments.save(Payment.pending(order.getId(), isFake() ? FAKE : ECPAY, tradeNo,
				BigDecimal.valueOf(order.getTotalTwd()), "TWD"));
		if (isFake()) {
			return PayStart.fake(tradeNo);
		}
		String back = siteUrl + "/pay/result?order=" + order.getOrderNo();
		return PayStart.ecpay(tradeNo, ecpay.build(tradeNo, order.getTotalTwd(), "Argo " + order.getOrderNo(),
				apiUrl + "/api/payments/ecpay/notify", apiUrl + "/api/payments/ecpay/return", back));
	}

	// 開發模式下由模擬頁回報結果，需訂單編號與 Email 都對得上
	@Transactional(noRollbackFor = ApiException.class)
	public OrderView completeFake(String orderNo, String email, String tradeNo, boolean success) {
		if (!isFake()) {
			throw new ApiException(ErrorCode.PAYMENT_UNAVAILABLE);
		}
		ShopOrder order = orderService.findForUpdate(orderNo, email);
		Payment p = payments.findByTradeNo(tradeNo)
				.filter(x -> x.getOrderId().equals(order.getId()) && FAKE.equals(x.getMethod()))
				.orElseThrow(() -> new ApiException(ErrorCode.ORDER_NOT_FOUND));
		apply(order, p, success, "fake_" + tradeNo, null, "FAKE_DECLINED");
		return orderService.view(order);
	}

	// 綠界通知，呼叫前需先驗過簽章
	@Transactional(noRollbackFor = ApiException.class)
	public Outcome complete(String tradeNo, boolean success, long amount, String gatewayNo, String last4,
			String failureCode) {
		Payment p = payments.findByTradeNo(tradeNo).orElse(null);
		if (p == null) {
			return new Outcome(false, null);
		}
		ShopOrder order = orders.findByIdForUpdate(p.getOrderId()).orElse(null);
		if (order == null) {
			return new Outcome(false, null);
		}
		if (order.getTotalTwd() == null || order.getTotalTwd() != amount) {
			log.error("payment amount mismatch trade={} expected={} got={}", tradeNo, order.getTotalTwd(), amount);
			if (Payment.PENDING.equals(p.getStatus())) {
				p.fail("AMOUNT_MISMATCH");
			}
			return new Outcome(false, order.getOrderNo());
		}
		apply(order, p, success, gatewayNo, last4, failureCode);
		return new Outcome(true, order.getOrderNo());
	}

	// 只有待付款的紀錄會變動，重複通知不會重複處理
	private void apply(ShopOrder order, Payment p, boolean success, String gatewayNo, String last4,
			String failureCode) {
		if (!Payment.PENDING.equals(p.getStatus())) {
			return;
		}
		if (!success) {
			p.fail(failureCode);
			return;
		}
		p.succeed(gatewayNo, last4);
		if (order.getStatus() == OrderStatus.PENDING_PAYMENT) {
			order.markPaid();
			events.publishEvent(OrderNotice.of(OrderNotice.Kind.PAID, order));
		} else {
			// 款已收但訂單已取消，需人工退款
			p.requireRefund();
			log.error("payment succeeded for non-payable order {} status={}", order.getOrderNo(), order.getStatus());
		}
	}
}
