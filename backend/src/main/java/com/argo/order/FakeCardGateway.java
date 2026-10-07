package com.argo.order;

import com.argo.common.ErrorCode;
import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;

// 假信用卡，規則仿常見的測試卡
@Component
public class FakeCardGateway implements PaymentGateway {

	// 特定卡號固定失敗
	static final Map<String, ErrorCode> FAILING = Map.of(
			"4000000000000002", ErrorCode.CARD_DECLINED,
			"4000000000009995", ErrorCode.INSUFFICIENT_FUNDS,
			"4000000000000119", ErrorCode.PROCESSING_ERROR);

	@Override
	public String method() {
		return "FAKE_CARD";
	}

	@Override
	public Result charge(String cardNumber, BigDecimal amount, String currency) {
		ErrorCode failure = FAILING.get(cardNumber);
		if (failure != null) {
			return Result.fail(failure);
		}
		return Result.ok("fake_" + UUID.randomUUID().toString().replace("-", "").substring(0, 24));
	}
}
