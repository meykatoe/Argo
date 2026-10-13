package com.argo.monitor;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

// 百分比或毫秒
@ConfigurationProperties("argo.monitor")
public record MonitorThresholds(
		@DefaultValue("80") double poolWarn, @DefaultValue("95") double poolDanger,
		@DefaultValue("85") double heapWarn, @DefaultValue("95") double heapDanger,
		@DefaultValue("85") double cpuWarn, @DefaultValue("95") double cpuDanger,
		@DefaultValue("2000") double p95WarnMs, @DefaultValue("5000") double p95DangerMs,
		@DefaultValue("2") double errorWarn, @DefaultValue("5") double errorDanger,
		@DefaultValue("20") long minRequests, @DefaultValue("6") int sustainSamples,
		@DefaultValue("5") int alertWindowMinutes) {
}
