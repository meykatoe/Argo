package com.argo.order;

import java.util.Optional;

// 向金流查詢交易結果
public interface PaymentQuery {

	// status 1 已付款，10200095 失敗，其他視為未定
	record Info(String status, long amount, String gatewayNo, String last4) {
	}

	// 查不到或查詢失敗回傳空
	Optional<Info> query(String tradeNo);
}
