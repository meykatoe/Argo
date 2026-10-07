package com.argo.card.admin;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record ExtraDiscountRequest(
		@NotNull @DecimalMin("0.0001") @DecimalMax("1") @Digits(integer = 1, fraction = 4) BigDecimal extraDiscount) {
}
