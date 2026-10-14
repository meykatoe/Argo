package com.argo.order;

import java.math.BigDecimal;

// 匯率來源
public interface RateSource {

	BigDecimal usdToTwd();
}
