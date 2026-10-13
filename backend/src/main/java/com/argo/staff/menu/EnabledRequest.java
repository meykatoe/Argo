package com.argo.staff.menu;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

// enabled：1 啟用、0 停用
public record EnabledRequest(@NotNull @Min(0) @Max(1) Integer enabled) {
}
