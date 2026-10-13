package com.argo.monitor;

import java.time.Clock;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

// 請求統計
@Component
public class RequestStats {

	static final int MAX_MINUTES = 60;
	static final long[] BOUNDS_MS = {5, 10, 25, 50, 100, 250, 500, 1000, 2500, 5000, 10000};
	private static final int MAX_SERIES = 2000;

	record Key(String app, String method, String path) {
	}

	static final class Agg {

		long count;
		long errors5xx;
		long errors4xx;
		long sumNanos;
		long maxNanos;
		final long[] hist = new long[BOUNDS_MS.length + 1];

		void clear() {
			count = 0;
			errors5xx = 0;
			errors4xx = 0;
			sumNanos = 0;
			maxNanos = 0;
			java.util.Arrays.fill(hist, 0);
		}

		void record(int status, long nanos) {
			count++;
			sumNanos += nanos;
			maxNanos = Math.max(maxNanos, nanos);
			if (status >= 500) {
				errors5xx++;
			}
			else if (status >= 400) {
				errors4xx++;
			}
			double ms = nanos / 1e6;
			int i = 0;
			while (i < BOUNDS_MS.length && ms > BOUNDS_MS[i]) {
				i++;
			}
			hist[i]++;
		}

		void add(Agg o) {
			count += o.count;
			errors5xx += o.errors5xx;
			errors4xx += o.errors4xx;
			sumNanos += o.sumNanos;
			maxNanos = Math.max(maxNanos, o.maxNanos);
			for (int i = 0; i < hist.length; i++) {
				hist[i] += o.hist[i];
			}
		}

		double avgMs() {
			return count == 0 ? 0 : sumNanos / 1e6 / count;
		}

		double maxMs() {
			return maxNanos / 1e6;
		}

		double errorRate() {
			return count == 0 ? 0 : errors5xx * 100.0 / count;
		}

		// 估算上界
		double percentileMs(double q) {
			if (count == 0) {
				return 0;
			}
			long target = (long) Math.ceil(q * count);
			long seen = 0;
			for (int i = 0; i < hist.length; i++) {
				seen += hist[i];
				if (seen >= target) {
					double upper = i < BOUNDS_MS.length ? BOUNDS_MS[i] : maxMs();
					return Math.min(upper, maxMs());
				}
			}
			return maxMs();
		}
	}

	private static final class Bucket {

		long minute = -1;
		final Agg agg = new Agg();
	}

	private static final class Series {

		final Bucket[] buckets = new Bucket[MAX_MINUTES];

		Series() {
			for (int i = 0; i < buckets.length; i++) {
				buckets[i] = new Bucket();
			}
		}
	}

	private final Clock clock;
	private final ConcurrentHashMap<Key, Series> series = new ConcurrentHashMap<>();

	@Autowired
	public RequestStats() {
		this(Clock.systemUTC());
	}

	RequestStats(Clock clock) {
		this.clock = clock;
	}

	public void record(String app, String method, String path, int status, long nanos) {
		long minute = clock.millis() / 60_000;
		Key key = new Key(app, method, path);
		Series s = series.get(key);
		if (s == null) {
			// 限制數量
			if (series.size() >= MAX_SERIES) {
				key = new Key(app, "*", "(other)");
			}
			s = series.computeIfAbsent(key, k -> new Series());
		}
		Bucket b = s.buckets[(int) (minute % MAX_MINUTES)];
		synchronized (b) {
			if (b.minute != minute) {
				b.minute = minute;
				b.agg.clear();
			}
			b.agg.record(status, nanos);
		}
	}

	// 最近幾分鐘
	Map<Key, Agg> snapshot(int minutes) {
		int window = Math.min(Math.max(minutes, 1), MAX_MINUTES);
		long now = clock.millis() / 60_000;
		Map<Key, Agg> out = new HashMap<>();
		series.forEach((key, s) -> {
			Agg total = new Agg();
			for (Bucket b : s.buckets) {
				synchronized (b) {
					if (b.minute > now - window && b.minute <= now) {
						total.add(b.agg);
					}
				}
			}
			if (total.count > 0) {
				out.put(key, total);
			}
		});
		return out;
	}
}
