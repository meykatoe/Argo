package com.argo.order;

import com.argo.common.PageResult;
import com.argo.common.Result;
import com.argo.customer.CustomerAccount;
import com.argo.customer.CustomerAuthInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// 登入顧客的訂單，需要顧客令牌
@RestController
@RequestMapping("/api/me/orders")
public class MyOrdersController {

	private final OrderService orders;

	public MyOrdersController(OrderService orders) {
		this.orders = orders;
	}

	@GetMapping
	public Result<PageResult<OrderView>> list(@RequestParam(defaultValue = "1") int page,
			@RequestParam(defaultValue = "10") int size, HttpServletRequest req) {
		CustomerAccount c = (CustomerAccount) req.getAttribute(CustomerAuthInterceptor.CUSTOMER_ATTR);
		return Result.ok(orders.listMine(c.getId(), page, size));
	}

	// 可認領的訪客訂單數
	@GetMapping("/claimable")
	public Result<ClaimResult> claimable(HttpServletRequest req) {
		CustomerAccount c = (CustomerAccount) req.getAttribute(CustomerAuthInterceptor.CUSTOMER_ATTR);
		return Result.ok(new ClaimResult(orders.claimableCount(c.getEmail())));
	}

	// 認領同 Email 的訪客訂單，可重複呼叫
	@PostMapping("/claim")
	public Result<ClaimResult> claim(HttpServletRequest req) {
		CustomerAccount c = (CustomerAccount) req.getAttribute(CustomerAuthInterceptor.CUSTOMER_ATTR);
		return Result.ok(new ClaimResult(orders.claimGuestOrders(c.getId(), c.getEmail())));
	}
}
