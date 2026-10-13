package com.argo.monitor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.argo.monitor.MonitorView.AlertView;
import com.argo.monitor.MonitorView.Level;
import com.argo.monitor.SystemSampler.Sample;
import java.util.List;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.StaticListableBeanFactory;

class MonitorServiceTests {

	static final long MS = 1_000_000L;

	final MonitorThresholds t = new MonitorThresholds(80, 95, 85, 95, 85, 95, 2000, 5000, 2, 5, 20, 3, 5);
	final RequestStats stats = new RequestStats();
	final SystemSampler sampler = new SystemSampler(new StaticListableBeanFactory().getBeanProvider(DataSource.class));
	final MonitorService service = new MonitorService(stats, sampler, t);

	void samples(double cpu, double heap, int poolActive, int pending, int n) {
		for (int i = 0; i < n; i++) {
			sampler.add(new Sample(System.currentTimeMillis(), cpu, heap, 100, 1000, 40, poolActive, 10, pending, 0));
		}
	}

	AlertView alert(String code) {
		return service.alerts().stream().filter(a -> a.code().equals(code)).findFirst().orElse(null);
	}

	@Test
	void quietSystemIsNormal() {
		samples(10, 30, 1, 0, 6);
		assertEquals(Level.NORMAL, service.view(5).level());
		assertTrue(service.view(5).alerts().isEmpty());
	}

	@Test
	void poolAboveWarnIsWarningAndAboveDangerIsDanger() {
		samples(10, 30, 8, 0, 3);
		assertEquals(Level.WARNING, alert("DB_POOL_HIGH").level());
		samples(10, 30, 10, 0, 3);
		assertEquals(Level.DANGER, alert("DB_POOL_HIGH").level());
	}

	@Test
	void shortSpikeDoesNotAlert() {
		samples(10, 30, 1, 0, 2);
		samples(10, 30, 10, 0, 1);
		assertEquals(null, alert("DB_POOL_HIGH"));
	}

	@Test
	void notEnoughSamplesNeverAlerts() {
		samples(99, 99, 10, 5, 2);
		assertTrue(service.alerts().isEmpty());
	}

	@Test
	void waitingForConnectionsIsDanger() {
		samples(10, 30, 5, 2, 3);
		assertEquals(Level.DANGER, alert("DB_POOL_WAITING").level());
	}

	@Test
	void heapAndCpuUseTheirOwnThresholds() {
		samples(90, 90, 1, 0, 3);
		assertEquals(Level.WARNING, alert("HEAP_HIGH").level());
		assertEquals(Level.WARNING, alert("CPU_HIGH").level());
	}

	@Test
	void slowAppAlertsOnlyWithEnoughRequests() {
		for (int i = 0; i < 19; i++) {
			stats.record("official-site", "GET", "/api/slow", 200, 3000 * MS);
		}
		assertEquals(null, alert("LATENCY_HIGH"));
		stats.record("official-site", "GET", "/api/slow", 200, 3000 * MS);
		AlertView a = alert("LATENCY_HIGH");
		assertEquals(Level.WARNING, a.level());
		assertEquals("official-site", a.app());
	}

	@Test
	void errorRateIsPerApp() {
		for (int i = 0; i < 18; i++) {
			stats.record("admin", "GET", "/api/ok", 200, MS);
			stats.record("official-site", "GET", "/api/ok", 200, MS);
		}
		for (int i = 0; i < 2; i++) {
			stats.record("admin", "GET", "/api/bad", 500, MS);
			stats.record("official-site", "GET", "/api/bad", 200, MS);
		}
		AlertView a = alert("ERROR_RATE_HIGH");
		assertEquals("admin", a.app());
		assertEquals(Level.DANGER, a.level());
		assertEquals(10.0, a.value(), 0.001);
	}

	@Test
	void viewListsAppsAndWorstEndpoints() {
		for (int i = 0; i < 5; i++) {
			stats.record("admin", "GET", "/api/fast", 200, 5 * MS);
			stats.record("admin", "GET", "/api/slow", 200, 800 * MS);
		}
		stats.record("ops", "GET", "/api/boom", 500, MS);
		MonitorView v = service.view(5);
		assertEquals("admin", v.apps().get(0).app());
		assertEquals(10, v.apps().get(0).requests());
		assertEquals("/api/slow", v.slowest().get(0).path());
		assertEquals("/api/boom", v.failing().get(0).path());
		assertEquals(1, v.failing().size());
	}
}
