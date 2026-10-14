package com.argo.order;

import java.util.Map;

// 開始付款的結果：綠界要前端送出表單，假金流走站內模擬頁
public record PayStart(String mode, String tradeNo, String action, Map<String, String> fields) {

	public static PayStart ecpay(String tradeNo, EcpayCheckout.Form form) {
		return new PayStart("ECPAY", tradeNo, form.action(), form.fields());
	}

	public static PayStart fake(String tradeNo) {
		return new PayStart("FAKE", tradeNo, null, null);
	}
}
