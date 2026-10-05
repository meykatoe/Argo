package com.argo.order;

import java.math.BigDecimal;

// 付款閘道，之後可換成綠界
public interface PaymentGateway {

	String method();

	Result charge(String cardNumber, BigDecimal amount, String currency);

	record Result(boolean success, String transactionId, String failureCode) {

		public static Result ok(String transactionId) {
			return new Result(true, transactionId, null);
		}

		public static Result fail(String code) {
			return new Result(false, null, code);
		}
	}
}
