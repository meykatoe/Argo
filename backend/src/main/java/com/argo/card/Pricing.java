package com.argo.card;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class Pricing {

	private Pricing() {
	}

	// 市價乘倍率，取兩位小數
	public static BigDecimal salePrice(BigDecimal marketPrice, BigDecimal rate) {
		return marketPrice.multiply(rate).setScale(2, RoundingMode.HALF_UP);
	}
}
