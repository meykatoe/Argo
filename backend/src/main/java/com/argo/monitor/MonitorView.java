package com.argo.monitor;

import java.util.List;

public record MonitorView(Level level, int windowMinutes, SystemView system, List<AppStat> apps,
		List<EndpointStat> slowest, List<EndpointStat> failing, List<AlertView> alerts,
		List<HistoryPoint> history) {

	public enum Level {
		NORMAL, WARNING, DANGER
	}

	public record SystemView(double cpuPercent, double heapPercent, long heapUsedMb, long heapMaxMb,
			int threads, int poolActive, int poolMax, int poolPending, double poolPercent,
			double gcPercent, long uptimeSeconds) {
	}

	public record AppStat(String app, long requests, double perMinute, long errors5xx, double errorRate,
			double avgMs, double p95Ms, double p99Ms, double maxMs) {
	}

	public record EndpointStat(String app, String method, String path, long requests, long errors5xx,
			double errorRate, double avgMs, double p95Ms, double maxMs) {
	}

	public record AlertView(String code, Level level, String app, double value, double threshold) {
	}

	public record HistoryPoint(long at, double cpu, double heap, double pool, double gc) {
	}
}
