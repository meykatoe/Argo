package com.argo.staff.audit;

import com.argo.staff.auth.StaffAccount;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

// 稽核紀錄不可含密碼與令牌
@Service
public class AuditLogService {

	private static final int MAX_USERNAME = 50;
	private static final int MAX_AGENT = 300;

	private final AuditLogRepository repo;

	public AuditLogService(AuditLogRepository repo) {
		this.repo = repo;
	}

	// 有外層交易時一起提交，操作與紀錄同生同滅
	@Transactional
	public void record(StaffAccount actor, String username, AuditAction action, boolean success,
			String targetType, String targetId, Map<String, Object> detail) {
		String ip = null;
		String agent = null;
		if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs) {
			HttpServletRequest req = attrs.getRequest();
			ip = req.getRemoteAddr();
			agent = cut(req.getHeader("User-Agent"), MAX_AGENT);
		}
		repo.save(new AuditLog(actor == null ? null : actor.getId(),
				cut(actor == null ? username : actor.getUsername(), MAX_USERNAME),
				actor == null ? null : actor.getRole(), action, success, targetType, targetId,
				detail == null || detail.isEmpty() ? null : detail, ip, agent));
	}

	private static String cut(String s, int max) {
		if (s == null) {
			return null;
		}
		return s.length() <= max ? s : s.substring(0, max);
	}
}
