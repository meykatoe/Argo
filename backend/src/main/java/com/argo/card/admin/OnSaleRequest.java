package com.argo.card.admin;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

// 1 上架、0 下架
public record OnSaleRequest(@NotNull @Min(0) @Max(1) Integer onSale) {
}
