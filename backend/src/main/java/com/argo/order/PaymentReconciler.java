package com.argo.order;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

// 通知遺失時主動向綠界對帳，結果走同一套 complete
@Service
public class PaymentReconciler {

	private static final Logger log = LoggerFactory.getLogger(PaymentReconciler.class);
	static final String PAID = "1";
	static final String FAILED = "10200095";

	private final PaymentRepository payments;
	private final PaymentService service;
	private final PaymentQuery query;
	private final int waitMinutes;
	private final int expireMinutes;

	public PaymentReconciler(PaymentRepository payments, PaymentService service, PaymentQuery query,
			@Value("${argo.payment.reconcile-minutes}") int waitMinutes,
			@Value("${argo.order.expire-minutes}") int expireMinutes) {
		this.payments = payments;
		this.service = service;
		this.query = query;
		this.waitMinutes = waitMinutes;
		this.expireMinutes = expireMinutes;
	}

	// 不丟例外，回傳已確定結果的筆數
	public int run() {
		try {
			OffsetDateTime now = OffsetDateTime.now();
			List<Payment> pending = payments.findReconcilable(PaymentService.ECPAY, OrderStatus.PENDING_PAYMENT,
					now.minusMinutes(waitMinutes), now.minusMinutes(expireMinutes));
			int done = 0;
			for (Payment p : pending) {
				if (reconcile(p.getTradeNo())) {
					done++;
				}
			}
			if (done > 0) {
				log.info("對帳補回 {} 筆付款結果", done);
			}
			return done;
		} catch (Exception e) {
			log.warn("payment reconcile failed", e);
			return 0;
		}
	}

	boolean reconcile(String tradeNo) {
		try {
			Optional<PaymentQuery.Info> info = query.query(tradeNo);
			if (info.isEmpty()) {
				return false;
			}
			PaymentQuery.Info i = info.get();
			if (PAID.equals(i.status())) {
				return service.complete(tradeNo, true, i.amount(), i.gatewayNo(), i.last4(), null).accepted();
			}
			if (FAILED.equals(i.status())) {
				return service.complete(tradeNo, false, i.amount(), i.gatewayNo(), i.last4(), "ECPAY_" + FAILED)
						.accepted();
			}
			return false;
		} catch (Exception e) {
			log.warn("reconcile failed trade={}", tradeNo, e);
			return false;
		}
	}
}
