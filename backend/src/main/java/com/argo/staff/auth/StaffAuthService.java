package com.argo.staff.auth;

import com.argo.common.ApiException;
import com.argo.common.ErrorCode;
import com.argo.common.PasswordPolicy;
import com.argo.staff.audit.AuditAction;
import com.argo.staff.audit.AuditLogService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StaffAuthService {

	private static final Pattern USERNAME = Pattern.compile("^[a-z0-9._-]{3,50}$");

	private final StaffAccountRepository accounts;
	private final StaffSessionRepository sessions;
	private final AuditLogService audit;
	private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(12);
	private final SecureRandom random = new SecureRandom();
	// 帳號不存在時也做一次比對，避免時間差洩漏
	private final String dummyHash = encoder.encode("dummy-password");
	private final int sessionHours;
	private final int maxFailures;
	private final int lockMinutes;

	public StaffAuthService(StaffAccountRepository accounts, StaffSessionRepository sessions,
			AuditLogService audit,
			@Value("${argo.admin.session-hours:8}") int sessionHours,
			@Value("${argo.admin.max-failures:5}") int maxFailures,
			@Value("${argo.admin.lock-minutes:15}") int lockMinutes) {
		this.accounts = accounts;
		this.sessions = sessions;
		this.audit = audit;
		this.sessionHours = sessionHours;
		this.maxFailures = maxFailures;
		this.lockMinutes = lockMinutes;
	}

	// 由人工指令建立，不對外開放
	@Transactional
	public StaffAccount create(String username, String password, StaffRole role) {
		String name = normalize(username);
		if (!USERNAME.matcher(name).matches()) {
			throw new IllegalArgumentException("username must be 3-50 chars of a-z 0-9 . _ -");
		}
		if (!PasswordPolicy.staffOk(password)) {
			throw new IllegalArgumentException(
					"password must be at least 8 chars with upper, lower and digit");
		}
		if (accounts.findByUsername(name).isPresent()) {
			throw new IllegalArgumentException("username already exists");
		}
		return accounts.save(new StaffAccount(name, encoder.encode(password), role));
	}

	// 失敗要記錄，不可隨例外回滾
	@Transactional(noRollbackFor = ApiException.class)
	public LoginResponse login(String username, String password, LoginPortal portal) {
		OffsetDateTime now = OffsetDateTime.now();
		Optional<StaffAccount> found = accounts.findByUsernameForUpdate(normalize(username));
		if (found.isEmpty()) {
			encoder.matches(password, dummyHash);
			audit.record(null, normalize(username), AuditAction.LOGIN_FAILED, false, null, null,
					Map.of("reason", "UNKNOWN_USER"));
			throw new ApiException(ErrorCode.LOGIN_FAILED);
		}
		StaffAccount staff = found.get();
		if (staff.isLocked(now)) {
			audit.record(staff, null, AuditAction.LOGIN_LOCKED, false, null, null,
					Map.of("reason", "STILL_LOCKED"));
			throw locked(staff, now);
		}
		boolean ok = encoder.matches(password, staff.getPasswordHash());
		if (!ok) {
			// 超過允許次數就鎖定，這一次也直接告知已鎖定
			if (staff.recordFailure(maxFailures, now.plusMinutes(lockMinutes))) {
				audit.record(staff, null, AuditAction.LOGIN_LOCKED, false, null, null,
						Map.of("reason", "TOO_MANY_FAILURES", "lockMinutes", lockMinutes));
				throw locked(staff, now);
			}
			audit.record(staff, null, AuditAction.LOGIN_FAILED, false, null, null,
					Map.of("reason", "BAD_PASSWORD"));
			throw new ApiException(ErrorCode.LOGIN_FAILED);
		}
		if (staff.isDisabled()) {
			audit.record(staff, null, AuditAction.LOGIN_FAILED, false, null, null,
					Map.of("reason", "DISABLED"));
			throw new ApiException(ErrorCode.LOGIN_FAILED);
		}
		// 密碼正確但走錯入口，不累計失敗次數
		if (!portal.accepts(staff.getRole())) {
			audit.record(staff, null, AuditAction.LOGIN_FAILED, false, null, null,
					Map.of("reason", "WRONG_PORTAL", "portal", portal.name()));
			throw new ApiException(ErrorCode.LOGIN_FAILED);
		}
		staff.recordSuccess(now);
		audit.record(staff, null, AuditAction.LOGIN_SUCCESS, true, null, null, null);
		sessions.deleteExpired(now);
		String token = newToken();
		OffsetDateTime expires = now.plusHours(sessionHours);
		sessions.save(new StaffSession(hash(token), staff.getId(), expires));
		return new LoginResponse(token, staff.getUsername(), staff.getRole(), expires);
	}

	// 令牌有效且帳號啟用才回傳人員
	@Transactional(readOnly = true)
	public Optional<StaffAccount> authenticate(String token) {
		if (token == null || token.isBlank()) {
			return Optional.empty();
		}
		return sessions.findById(hash(token))
				.filter(s -> s.getExpiresAt().isAfter(OffsetDateTime.now()))
				.flatMap(s -> accounts.findById(s.getStaffId()))
				.filter(s -> !s.isDisabled());
	}

	@Transactional
	public void logout(String token) {
		if (token != null) {
			authenticate(token).ifPresent(
					s -> audit.record(s, null, AuditAction.LOGOUT, true, null, null, null));
			sessions.deleteById(hash(token));
		}
	}

	// 回應帶上還要等幾秒，前端可顯示分鐘數
	private static ApiException locked(StaffAccount staff, OffsetDateTime now) {
		return new ApiException(ErrorCode.LOGIN_LOCKED,
				Map.of("retryAfterSeconds", String.valueOf(staff.retryAfterSeconds(now))));
	}

	private static String normalize(String username) {
		return username == null ? "" : username.trim().toLowerCase(Locale.ROOT);
	}

	private String newToken() {
		byte[] bytes = new byte[32];
		random.nextBytes(bytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}

	private static String hash(String token) {
		try {
			byte[] d = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(d);
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
	}
}
