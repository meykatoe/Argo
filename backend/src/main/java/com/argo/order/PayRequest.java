package com.argo.order;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record PayRequest(@NotBlank @Email String email, @NotNull @Valid Card card) {

	public record Card(
			@NotBlank @Size(max = 25) String number,
			@Min(1) @Max(12) int expMonth,
			@Min(2000) @Max(2200) int expYear,
			@NotBlank @Pattern(regexp = "^[0-9]{3,4}$") String cvc,
			@NotBlank @Size(max = 100) String holderName) {

		// 避免卡號與安全碼出現在日誌
		@Override
		public String toString() {
			return "Card[****]";
		}
	}
}
