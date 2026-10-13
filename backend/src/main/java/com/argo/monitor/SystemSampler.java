package com.argo.monitor;

import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryUsage;
import java.sql.SQLException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

// 定時取樣
@Component
public class SystemSampler {

	private static final Logger log = LoggerFactory.getLogger(SystemSampler.class);
	static final int HISTORY = 360;

	record Sample(long at, double cpu, double heap, long heapUsedMb, long heapMaxMb, int threads,
			int poolActive, int poolMax, int poolPending, double gc) {

		double pool() {
			return poolMax <= 0 ? 0 : poolActive * 100.0 / poolMax;
		}
	}

	private final ObjectProvider<DataSource> dataSource;
	private final Deque<Sample> samples = new ArrayDeque<>();
	private long lastGcMs = -1;
	private long lastAt;

	public SystemSampler(ObjectProvider<DataSource> dataSource) {
		this.dataSource = dataSource;
	}

	@Scheduled(fixedDelayString = "${argo.monitor.sample-millis:10000}")
	public void sample() {
		try {
			add(read());
		}
		catch (RuntimeException e) {
			// 取樣失敗不影響服務
			log.warn("Monitor sample failed: {}", e.getMessage());
		}
	}

	void add(Sample s) {
		synchronized (samples) {
			samples.addLast(s);
			while (samples.size() > HISTORY) {
				samples.removeFirst();
			}
		}
	}

	// 最近 n 筆
	List<Sample> last(int n) {
		synchronized (samples) {
			return samples.stream().skip(Math.max(0, samples.size() - n)).toList();
		}
	}

	List<Sample> since(long fromMs) {
		synchronized (samples) {
			return samples.stream().filter(s -> s.at() >= fromMs).toList();
		}
	}

	Sample latest() {
		synchronized (samples) {
			return samples.peekLast();
		}
	}

	private Sample read() {
		long now = System.currentTimeMillis();
		MemoryUsage heap = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage();
		long max = heap.getMax() > 0 ? heap.getMax() : heap.getCommitted();
		double cpu = 0;
		if (ManagementFactory.getOperatingSystemMXBean() instanceof com.sun.management.OperatingSystemMXBean os) {
			double load = os.getProcessCpuLoad();
			cpu = load < 0 ? 0 : load * 100;
		}
		long gcMs = 0;
		for (GarbageCollectorMXBean gc : ManagementFactory.getGarbageCollectorMXBeans()) {
			gcMs += Math.max(0, gc.getCollectionTime());
		}
		double gcPercent = 0;
		if (lastGcMs >= 0 && now > lastAt) {
			gcPercent = Math.min(100, Math.max(0, (gcMs - lastGcMs) * 100.0 / (now - lastAt)));
		}
		lastGcMs = gcMs;
		lastAt = now;

		int active = 0;
		int poolMax = 0;
		int waiting = 0;
		DataSource ds = dataSource.getIfAvailable();
		try {
			if (ds != null && ds.isWrapperFor(HikariDataSource.class)) {
				HikariDataSource hikari = ds.unwrap(HikariDataSource.class);
				poolMax = hikari.getMaximumPoolSize();
				HikariPoolMXBean pool = hikari.getHikariPoolMXBean();
				if (pool != null) {
					active = pool.getActiveConnections();
					waiting = pool.getThreadsAwaitingConnection();
				}
			}
		}
		catch (SQLException e) {
			log.warn("Monitor pool read failed: {}", e.getMessage());
		}
		long mb = 1024 * 1024;
		return new Sample(now, cpu, heap.getUsed() * 100.0 / max, heap.getUsed() / mb, max / mb,
				ManagementFactory.getThreadMXBean().getThreadCount(), active, poolMax, waiting, gcPercent);
	}
}
