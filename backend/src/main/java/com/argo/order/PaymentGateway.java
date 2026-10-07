package com.argo.order;

import com.argo.common.ErrorCode;
import java.math.BigDecimal;

// 付款閘道，之後可換成綠界
public interface PaymentGateway {

	String method();

	Result charge(String cardNumber, BigDecimal amount, String currency);

	record Result(boolean success, String transactionId, ErrorCode failure) {

		public String failureCode() {
			return failure == null ? null : failure.name();
		}

		public static Result ok(String transactionId) {
			return new Result(true, transactionId, null);
		}

		public static Result fail(ErrorCode code) {
			return new Result(false, null, code);
		}
	}
}
