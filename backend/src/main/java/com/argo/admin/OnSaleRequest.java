package com.argo.admin;

import jakarta.validation.constraints.NotNull;

public record OnSaleRequest(@NotNull Boolean onSale) {
}
