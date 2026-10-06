package com.argo.security;

import com.argo.admin.AuditAction;
import com.argo.admin.AuditLogService;
import com.argo.admin.StaffAccount;
import com.argo.common.ApiException;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 自動封鎖的規則與白名單，存在資料庫，短時間快取，修改後立即重讀
@Service
public class AutoBlockPolicyService {

	public record Rule(boolean enabled, int threshold, int windowMinutes, int blockHours) {
	}

	private final IpAutoBlockRuleRepository rules;
	private final IpAllowEntryRepository allow;
	private final AuditLogService audit;
	private final long cacheMillis;

	private volatile Map<AutoBlockMetric, Rule> ruleSnapshot = new EnumMap<>(AutoBlockMetric.class);
	private volatile List<String> allowSnapshot = List.of();
	private volatile long loadedAt;

	public AutoBlockPolicyService(IpAutoBlockRuleRepository rules, IpAllowEntryRepository allow,
			AuditLogService audit, @Value("${argo.security.block-cache-millis:5000}") long cacheMillis) {
		this.rules = rules;
		this.allow = allow;
		this.audit = audit;
		this.cacheMillis = cacheMillis;
	}

	public Rule rule(AutoBlockMetric metric) {
		refreshIfStale();
		return ruleSnapshot.get(metric);
	}

	public boolean isAllowed(String ip) {
		refreshIfStale();
		return allowSnapshot.stream().anyMatch(rule -> Ips.matches(ip, rule));
	}

	private void refreshIfStale() {
		if (System.currentTimeMillis() - loadedAt < cacheMillis) {
			return;
		}
		synchronized (this) {
			if (System.currentTimeMillis() - loadedAt >= cacheMillis) {
				reload();
			}
		}
	}

	private void reload() {
		Map<AutoBlockMetric, Rule> fresh = new EnumMap<>(AutoBlockMetric.class);
		for (IpAutoBlockRule r : rules.findAll()) {
			fresh.put(r.getMetric(), new Rule(r.isEnabled(), r.getThreshold(), r.getWindowMinutes(), r.getBlockHours()));
		}
		ruleSnapshot = fresh;
		allowSnapshot = allow.findAll().stream().map(IpAllowEntry::getIp).toList();
		loadedAt = System.currentTimeMillis();
	}

	public synchronized void refreshNow() {
		reload();
	}

	@Transactional(readOnly = true)
	public List<IpAutoBlockRule> listRules() {
		return rules.findAll().stream().sorted((a, b) -> a.getMetric().compareTo(b.getMetric())).toList();
	}

	@Transactional(readOnly = true)
	public List<IpAllowEntry> listAllow() {
		return allow.findAll().stream().sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt())).toList();
	}

	@Transactional
	public IpAutoBlockRule updateRule(StaffAccount actor, AutoBlockMetric metric, boolean enabled, int threshold,
			int windowMinutes, int blockHours) {
		IpAutoBlockRule r = rules.findByMetric(metric)
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "RULE_NOT_FOUND"));
		Map<String, Object> detail = new LinkedHashMap<>();
		detail.put("metric", metric.name());
		detail.put("before", describe(r.isEnabled(), r.getThreshold(), r.getWindowMinutes(), r.getBlockHours()));
		r.update(enabled, threshold, windowMinutes, blockHours, actor.getUsername());
		detail.put("after", describe(enabled, threshold, windowMinutes, blockHours));
		audit.record(actor, null, AuditAction.IP_RULE_UPDATED, true, "IP_RULE", metric.name(), detail);
		rules.flush();
		refreshNow();
		return r;
	}

	private static String describe(boolean enabled, int threshold, int window, int hours) {
		return (enabled ? "on" : "off") + ":" + threshold + "/" + window + "min->" + hours + "h";
	}

	@Transactional
	public IpAllowEntry addAllow(StaffAccount actor, String rawIp, String note) {
		String rule = Ips.normalizeRule(rawIp);
		if (rule == null) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_IP");
		}
		IpAllowEntry e = allow.save(new IpAllowEntry(rule, note.trim(), actor.getUsername()));
		audit.record(actor, null, AuditAction.IP_ALLOWLIST_ADDED, true, "IP", rule, Map.of("note", e.getNote()));
		allow.flush();
		refreshNow();
		return e;
	}

	@Transactional
	public void removeAllow(StaffAccount actor, String rawIp) {
		String rule = Ips.normalizeRule(rawIp);
		if (rule == null) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_IP");
		}
		IpAllowEntry e = allow.findById(rule).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "ALLOW_NOT_FOUND"));
		allow.delete(e);
		audit.record(actor, null, AuditAction.IP_ALLOWLIST_REMOVED, true, "IP", rule, Map.of("note", e.getNote()));
		allow.flush();
		refreshNow();
	}
}
