package com.argo.order;

import com.argo.common.Result;
import com.argo.customer.CustomerAccount;
import com.argo.customer.CustomerAuthInterceptor;
import com.argo.customer.CustomerAuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

	private final OrderService orders;
	private final PaymentService payments;
	private final CustomerAuthService customers;

	public OrderController(OrderService orders, PaymentService payments, CustomerAuthService customers) {
		this.orders = orders;
		this.payments = payments;
		this.customers = customers;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public Result<OrderView> create(@Valid @RequestBody CreateOrderRequest req,
			@RequestParam(defaultValue = "en") String lang, HttpServletRequest http) {
		// 有登入就綁定顧客，沒登入或令牌失效都當訪客，結帳不因此失敗
		Long customerId = customers.authenticate(CustomerAuthInterceptor.bearer(http))
				.map(CustomerAccount::getId).orElse(null);
		return Result.ok(orders.create(req, lang, customerId));
	}

	@GetMapping("/{orderNo}")
	public Result<OrderView> get(@PathVariable String orderNo, @RequestParam String email) {
		return Result.ok(orders.get(orderNo, email));
	}

	@PostMapping("/{orderNo}/pay")
	public Result<OrderView> pay(@PathVariable String orderNo, @Valid @RequestBody PayRequest req) {
		return Result.ok(payments.pay(orderNo, req));
	}

	@PostMapping("/{orderNo}/cancel")
	public Result<OrderView> cancel(@PathVariable String orderNo, @Valid @RequestBody EmailRequest req) {
		return Result.ok(orders.cancel(orderNo, req.email()));
	}
}
