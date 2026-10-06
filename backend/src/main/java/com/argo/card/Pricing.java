package com.argo.card;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class Pricing {

	private Pricing() {
	}

	// 市價乘倍率，取兩位小數
	public static BigDecimal salePrice(BigDecimal marketPrice, BigDecimal rate) {
		return salePrice(marketPrice, rate, BigDecimal.ONE);
	}

	// 再乘額外折扣
	public static BigDecimal salePrice(BigDecimal marketPrice, BigDecimal rate, BigDecimal extra) {
		return marketPrice.multiply(rate).multiply(extra).setScale(2, RoundingMode.HALF_UP);
	}
}
