package com.argo.order;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;

// 假信用卡，規則仿常見的測試卡
@Component
public class FakeCardGateway implements PaymentGateway {

	// 特定卡號固定失敗
	static final Map<String, String> FAILING = Map.of(
			"4000000000000002", "CARD_DECLINED",
			"4000000000009995", "INSUFFICIENT_FUNDS",
			"4000000000000119", "PROCESSING_ERROR");

	@Override
	public String method() {
		return "FAKE_CARD";
	}

	@Override
	public Result charge(String cardNumber, BigDecimal amount, String currency) {
		String failure = FAILING.get(cardNumber);
		if (failure != null) {
			return Result.fail(failure);
		}
		return Result.ok("fake_" + UUID.randomUUID().toString().replace("-", "").substring(0, 24));
	}
}
