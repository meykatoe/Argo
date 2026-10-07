package com.argo.security.web;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

// enabled：1 啟用、0 停用
public record UpdateRuleRequest(
		@NotNull @Min(0) @Max(1) Integer enabled,
		@NotNull @Min(2) @Max(10000) Integer threshold,
		@NotNull @Min(1) @Max(1440) Integer windowMinutes,
		@NotNull @Min(1) @Max(8760) Integer blockHours) {
}
