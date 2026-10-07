package com.argo.security;

import com.argo.common.ApiException;
import com.argo.common.ErrorCode;
import com.argo.staff.audit.AuditAction;
import com.argo.staff.audit.AuditLogService;
import com.argo.staff.auth.StaffAccount;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IpBlockService {

	public static final String AUTO_ACTOR = "system:auto-block";

	private final IpBlockRepository repo;
	private final AuditLogService audit;
	private final ClientIpResolver resolver;
	private final long cacheMillis;

	// 擋 IP 在每個請求都要查，所以用短時間的記憶體快取
	private volatile Map<String, OffsetDateTime> snapshot = Map.of();
	private volatile long loadedAt;

	public IpBlockService(IpBlockRepository repo, AuditLogService audit, ClientIpResolver resolver,
			@Value("${argo.security.block-cache-millis:5000}") long cacheMillis) {
		this.repo = repo;
		this.audit = audit;
		this.resolver = resolver;
		this.cacheMillis = cacheMillis;
	}

	// 沒有到期時間用最大值表示永久
	private static final OffsetDateTime FOREVER = OffsetDateTime.MAX;

	public boolean isBlocked(String ip) {
		refreshIfStale();
		OffsetDateTime until = snapshot.get(ip);
		return until != null && until.isAfter(OffsetDateTime.now());
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
		Map<String, OffsetDateTime> fresh = new HashMap<>();
		for (IpBlock b : repo.findActive(OffsetDateTime.now())) {
			fresh.put(b.getIp(), b.getExpiresAt() == null ? FOREVER : b.getExpiresAt());
		}
		snapshot = fresh;
		loadedAt = System.currentTimeMillis();
	}

	// 立刻重讀，讓封鎖與解除馬上生效
	public synchronized void refreshNow() {
		reload();
	}

	@Transactional(readOnly = true)
	public List<IpBlock> listActive() {
		return repo.findActive(OffsetDateTime.now());
	}

	// 目前有效封鎖的 IP 對應資料
	@Transactional(readOnly = true)
	public Map<String, IpBlock> activeByIp() {
		return repo.findActive(OffsetDateTime.now()).stream()
				.collect(Collectors.toMap(IpBlock::getIp, b -> b));
	}

	@Transactional
	public IpBlock block(StaffAccount actor, String rawIp, String reason, Integer hours, String requesterIp) {
		String ip = Ips.normalize(rawIp);
		if (ip == null) {
			throw new ApiException(ErrorCode.INVALID_IP);
		}
		// 本機、代理與自己的 IP 不可封鎖，否則可能把所有人或自己鎖在外面
		if (Ips.isSpecial(ip) || resolver.isTrustedProxy(ip)) {
			throw new ApiException(ErrorCode.PROTECTED_IP);
		}
		if (ip.equals(requesterIp)) {
			throw new ApiException(ErrorCode.CANNOT_BLOCK_SELF);
		}
		OffsetDateTime expires = hours == null ? null : OffsetDateTime.now().plusHours(hours);
		IpBlock b = repo.save(new IpBlock(ip, reason.trim(), actor.getUsername(), actor.getId(), expires, false));
		Map<String, Object> detail = new LinkedHashMap<>();
		detail.put("reason", b.getReason());
		detail.put("hours", hours == null ? "permanent" : String.valueOf(hours));
		audit.record(actor, null, AuditAction.IP_BLOCKED, true, "IP", ip, detail);
		repo.flush();
		refreshNow();
		return b;
	}

	// 系統自動封鎖：已經被封鎖的不重複處理，也不會蓋掉人工設定的封鎖
	@Transactional
	public synchronized boolean autoBlock(String ip, String reason, int hours, Map<String, Object> detail) {
		if (isBlocked(ip)) {
			return false;
		}
		repo.save(new IpBlock(ip, reason, AUTO_ACTOR, null, OffsetDateTime.now().plusHours(hours), true));
		Map<String, Object> d = new LinkedHashMap<>(detail);
		d.put("auto", 1);
		d.put("hours", String.valueOf(hours));
		d.put("reason", reason);
		audit.record(null, AUTO_ACTOR, AuditAction.IP_BLOCKED, true, "IP", ip, d);
		repo.flush();
		refreshNow();
		return true;
	}

	@Transactional
	public void unblock(StaffAccount actor, String rawIp) {
		String ip = Ips.normalize(rawIp);
		if (ip == null) {
			throw new ApiException(ErrorCode.INVALID_IP);
		}
		IpBlock b = repo.findById(ip).orElseThrow(() -> new ApiException(ErrorCode.IP_NOT_BLOCKED));
		repo.delete(b);
		audit.record(actor, null, AuditAction.IP_UNBLOCKED, true, "IP", ip,
				Map.of("reason", b.getReason(), "blockedBy", b.getBlockedBy()));
		repo.flush();
		refreshNow();
	}

	// 清掉已到期的封鎖
	@Scheduled(fixedDelay = 3_600_000, initialDelay = 60_000)
	@Transactional
	public void purgeExpired() {
		repo.deleteExpired(OffsetDateTime.now());
	}
}
