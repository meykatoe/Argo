package com.argo.security;

import java.util.ArrayDeque;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

// 某個 IP 在時間窗內累積到門檻就自動封鎖一段時間，到期由封鎖名單的期限自動解除
@Component
public class AutoBlocker {

	private static final Logger log = LoggerFactory.getLogger(AutoBlocker.class);

	private record Key(AutoBlockMetric metric, String ip) {
	}

	private final AutoBlockPolicyService policy;
	private final IpBlockService blocks;
	private final ClientIpResolver resolver;
	private final LongSupplier clock;
	// 每個 IP 只保留最近 threshold 次的時間，記憶體有上限
	private final ConcurrentHashMap<Key, ArrayDeque<Long>> hits = new ConcurrentHashMap<>();

	@Autowired
	public AutoBlocker(AutoBlockPolicyService policy, IpBlockService blocks, ClientIpResolver resolver) {
		this(policy, blocks, resolver, System::currentTimeMillis);
	}

	AutoBlocker(AutoBlockPolicyService policy, IpBlockService blocks, ClientIpResolver resolver, LongSupplier clock) {
		this.policy = policy;
		this.blocks = blocks;
		this.resolver = resolver;
		this.clock = clock;
	}

	// 出錯只記日誌，絕不能影響正常請求
	public void record(String ip, AutoBlockMetric metric) {
		try {
			handle(ip, metric);
		} catch (RuntimeException e) {
			log.warn("自動封鎖判斷失敗：{}", e.getMessage());
		}
	}

	private void handle(String ip, AutoBlockMetric metric) {
		AutoBlockPolicyService.Rule rule = policy.rule(metric);
		if (rule == null || !rule.enabled()) {
			return;
		}
		// 本機、代理、白名單永遠不自動封鎖
		if (Ips.isSpecial(ip) || resolver.isTrustedProxy(ip) || policy.isAllowed(ip)) {
			return;
		}
		long now = clock.getAsLong();
		long window = rule.windowMinutes() * 60_000L;
		ArrayDeque<Long> deque = hits.computeIfAbsent(new Key(metric, ip), k -> new ArrayDeque<>());
		boolean fire;
		synchronized (deque) {
			deque.addLast(now);
			while (deque.size() > rule.threshold()) {
				deque.removeFirst();
			}
			fire = deque.size() == rule.threshold() && now - deque.peekFirst() <= window;
			if (fire) {
				deque.clear();
			}
		}
		if (fire) {
			Map<String, Object> detail = new LinkedHashMap<>();
			detail.put("metric", metric.name());
			detail.put("threshold", rule.threshold());
			detail.put("windowMinutes", rule.windowMinutes());
			String reason = "自動封鎖：" + rule.windowMinutes() + " 分鐘內" + label(metric) + "達 " + rule.threshold() + " 次";
			blocks.autoBlock(ip, reason, rule.blockHours(), detail);
		}
	}

	private static String label(AutoBlockMetric m) {
		return m == AutoBlockMetric.RATE_LIMITED ? "被限速" : "登入失敗";
	}

	// 清掉太久沒動靜的紀錄，窗口最大 24 小時
	@Scheduled(fixedDelay = 300_000, initialDelay = 300_000)
	public void purge() {
		long now = clock.getAsLong();
		hits.entrySet().removeIf(e -> {
			synchronized (e.getValue()) {
				Long last = e.getValue().peekLast();
				return last == null || now - last > 24 * 3_600_000L;
			}
		});
	}

	int size() {
		return hits.size();
	}
}
