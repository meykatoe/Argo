package com.argo.staff.menu;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

// granted：1 授權、0 撤銷
public record GrantRequest(@NotNull @Min(0) @Max(1) Integer granted) {
}
