package com.argo.order;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

// 開發模式的模擬付款結果，success 為 1 或 0
public record FakeCompleteRequest(@NotBlank @Email String email, @NotBlank String tradeNo,
		@NotNull @Min(0) @Max(1) Integer success) {
}
