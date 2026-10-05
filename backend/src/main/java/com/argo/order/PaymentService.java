package com.argo.order;

import com.argo.common.ApiException;
import java.time.YearMonth;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentService {

	private final OrderService orderService;
	private final PaymentRepository payments;
	private final PaymentGateway gateway;

	public PaymentService(OrderService orderService, PaymentRepository payments, PaymentGateway gateway) {
		this.orderService = orderService;
		this.payments = payments;
		this.gateway = gateway;
	}

	// 付款失敗也要保留紀錄，所以業務錯誤不回滾
	@Transactional(noRollbackFor = ApiException.class)
	public OrderView pay(String orderNo, PayRequest req) {
		ShopOrder order = orderService.findForUpdate(orderNo, req.email());
		if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
			throw new ApiException(HttpStatus.CONFLICT, "ORDER_NOT_PAYABLE");
		}
		if (orderService.isExpired(order)) {
			orderService.cancelInternal(order, "EXPIRED");
			throw new ApiException(HttpStatus.GONE, "ORDER_EXPIRED");
		}

		String number = req.card().number().replaceAll("[ -]", "");
		validateCard(number, req.card());

		PaymentGateway.Result result = gateway.charge(number, order.getTotal(), order.getCurrency());
		String last4 = number.substring(number.length() - 4);
		payments.save(new Payment(order.getId(), gateway.method(),
				result.success() ? Payment.SUCCEEDED : Payment.FAILED, order.getTotal(),
				order.getCurrency(), last4, result.transactionId(), result.failureCode()));
		if (!result.success()) {
			throw new ApiException(HttpStatus.PAYMENT_REQUIRED, result.failureCode());
		}
		order.markPaid();
		return orderService.view(order);
	}

	private void validateCard(String number, PayRequest.Card card) {
		if (!number.matches("^[0-9]{13,19}$") || !luhn(number)) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_CARD");
		}
		if (YearMonth.of(card.expYear(), card.expMonth()).isBefore(YearMonth.now())) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "CARD_EXPIRED");
		}
	}

	// 卡號檢查碼
	static boolean luhn(String digits) {
		int sum = 0;
		boolean doubleIt = false;
		for (int i = digits.length() - 1; i >= 0; i--) {
			int d = digits.charAt(i) - '0';
			if (doubleIt) {
				d *= 2;
				if (d > 9) {
					d -= 9;
				}
			}
			sum += d;
			doubleIt = !doubleIt;
		}
		return sum % 10 == 0;
	}
}
