package com.argo.order;

import com.argo.common.Result;
import jakarta.validation.Valid;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// 正式環境不註冊
@Profile("!prod")
@RestController
@RequestMapping("/api/orders/{orderNo}/fake-pay")
public class FakePaymentController {

	private final PaymentService payments;

	public FakePaymentController(PaymentService payments) {
		this.payments = payments;
	}

	@PostMapping
	public Result<OrderView> complete(@PathVariable String orderNo, @Valid @RequestBody FakeCompleteRequest req) {
		return Result.ok(payments.completeFake(orderNo, req.email(), req.tradeNo(), req.success() == 1));
	}
}
