package com.argo.monitor;

import com.argo.monitor.MonitorView.AlertView;
import com.argo.monitor.MonitorView.AppStat;
import com.argo.monitor.MonitorView.EndpointStat;
import com.argo.monitor.MonitorView.HistoryPoint;
import com.argo.monitor.MonitorView.Level;
import com.argo.monitor.MonitorView.SystemView;
import com.argo.monitor.RequestStats.Agg;
import com.argo.monitor.RequestStats.Key;
import com.argo.monitor.SystemSampler.Sample;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.ToDoubleFunction;
import org.springframework.stereotype.Service;

@Service
public class MonitorService {

	private static final int TOP = 10;

	private final RequestStats stats;
	private final SystemSampler sampler;
	private final MonitorThresholds t;

	public MonitorService(RequestStats stats, SystemSampler sampler, MonitorThresholds t) {
		this.stats = stats;
		this.sampler = sampler;
		this.t = t;
	}

	public MonitorView view(int minutes) {
		int window = Math.min(Math.max(minutes, 1), RequestStats.MAX_MINUTES);
		Map<Key, Agg> snap = stats.snapshot(window);
		List<AlertView> alerts = alerts();
		Level level = alerts.stream().map(AlertView::level).max(Comparator.naturalOrder()).orElse(Level.NORMAL);
		long from = System.currentTimeMillis() - window * 60_000L;
		List<HistoryPoint> history = sampler.since(from).stream()
				.map(s -> new HistoryPoint(s.at(), r1(s.cpu()), r1(s.heap()), r1(s.pool()), r1(s.gc()))).toList();
		return new MonitorView(level, window, system(), apps(snap, window), slowest(snap), failing(snap),
				alerts, history);
	}

	private SystemView system() {
		Sample s = sampler.latest();
		long uptime = ManagementFactory.getRuntimeMXBean().getUptime() / 1000;
		if (s == null) {
			return new SystemView(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, uptime);
		}
		return new SystemView(r1(s.cpu()), r1(s.heap()), s.heapUsedMb(), s.heapMaxMb(), s.threads(),
				s.poolActive(), s.poolMax(), s.poolPending(), r1(s.pool()), r1(s.gc()), uptime);
	}

	private List<AppStat> apps(Map<Key, Agg> snap, int window) {
		return byApp(snap).entrySet().stream().map(e -> {
			Agg a = e.getValue();
			return new AppStat(e.getKey(), a.count, r1((double) a.count / window), a.errors5xx,
					r1(a.errorRate()), r1(a.avgMs()), r1(a.percentileMs(0.95)), r1(a.percentileMs(0.99)),
					r1(a.maxMs()));
		}).sorted(Comparator.comparingLong(AppStat::requests).reversed()).toList();
	}

	private static Map<String, Agg> byApp(Map<Key, Agg> snap) {
		Map<String, Agg> out = new TreeMap<>();
		snap.forEach((k, a) -> out.computeIfAbsent(k.app(), x -> new Agg()).add(a));
		return out;
	}

	private List<EndpointStat> slowest(Map<Key, Agg> snap) {
		return endpoints(snap, a -> a.count >= 3, EndpointStat::p95Ms);
	}

	private List<EndpointStat> failing(Map<Key, Agg> snap) {
		return endpoints(snap, a -> a.errors5xx > 0, e -> e.errors5xx());
	}

	private List<EndpointStat> endpoints(Map<Key, Agg> snap, java.util.function.Predicate<Agg> keep,
			ToDoubleFunction<EndpointStat> order) {
		List<EndpointStat> out = new ArrayList<>();
		snap.forEach((k, a) -> {
			if (keep.test(a)) {
				out.add(new EndpointStat(k.app(), k.method(), k.path(), a.count, a.errors5xx,
						r1(a.errorRate()), r1(a.avgMs()), r1(a.percentileMs(0.95)), r1(a.maxMs())));
			}
		});
		out.sort(Comparator.comparingDouble(order).reversed());
		return out.size() > TOP ? List.copyOf(out.subList(0, TOP)) : out;
	}

	List<AlertView> alerts() {
		List<AlertView> out = new ArrayList<>();
		systemAlerts(out);
		appAlerts(out);
		return out;
	}

	// 需連續超標
	private void systemAlerts(List<AlertView> out) {
		List<Sample> recent = sampler.last(t.sustainSamples());
		if (recent.size() < t.sustainSamples()) {
			return;
		}
		check(out, "DB_POOL_HIGH", null, floor(recent, Sample::pool), t.poolWarn(), t.poolDanger());
		check(out, "DB_POOL_WAITING", null, floor(recent, s -> s.poolPending()), 1, 1);
		check(out, "HEAP_HIGH", null, floor(recent, Sample::heap), t.heapWarn(), t.heapDanger());
		check(out, "CPU_HIGH", null, floor(recent, Sample::cpu), t.cpuWarn(), t.cpuDanger());
	}

	private void appAlerts(List<AlertView> out) {
		byApp(stats.snapshot(t.alertWindowMinutes())).forEach((app, a) -> {
			if (a.count < t.minRequests()) {
				return;
			}
			check(out, "LATENCY_HIGH", app, a.percentileMs(0.95), t.p95WarnMs(), t.p95DangerMs());
			check(out, "ERROR_RATE_HIGH", app, a.errorRate(), t.errorWarn(), t.errorDanger());
		});
	}

	private static double floor(List<Sample> samples, ToDoubleFunction<Sample> f) {
		return samples.stream().mapToDouble(f).min().orElse(0);
	}

	private static void check(List<AlertView> out, String code, String app, double value, double warn,
			double danger) {
		if (value >= danger) {
			out.add(new AlertView(code, Level.DANGER, app, r1(value), danger));
		}
		else if (value >= warn) {
			out.add(new AlertView(code, Level.WARNING, app, r1(value), warn));
		}
	}

	private static double r1(double v) {
		return Math.round(v * 10) / 10.0;
	}
}
