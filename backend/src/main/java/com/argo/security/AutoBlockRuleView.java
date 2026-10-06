package com.argo.security;

import com.argo.common.Flag;
import java.time.OffsetDateTime;

// enabled 為 1 代表啟用
public record AutoBlockRuleView(AutoBlockMetric metric, int enabled, int threshold, int windowMinutes,
		int blockHours, String updatedBy, OffsetDateTime updatedAt) {

	public static AutoBlockRuleView from(IpAutoBlockRule r) {
		return new AutoBlockRuleView(r.getMetric(), Flag.of(r.isEnabled()), r.getThreshold(), r.getWindowMinutes(),
				r.getBlockHours(), r.getUpdatedBy(), r.getUpdatedAt());
	}
}
