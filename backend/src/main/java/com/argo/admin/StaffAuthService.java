package com.argo.admin;

import com.argo.common.ApiException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StaffAuthService {

	public static final int MIN_PASSWORD = 10;
	private static final Pattern USERNAME = Pattern.compile("^[a-z0-9._-]{3,50}$");

	private final StaffAccountRepository accounts;
	private final StaffSessionRepository sessions;
	private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(12);
	private final SecureRandom random = new SecureRandom();
	// 帳號不存在時也做一次比對，避免時間差洩漏
	private final String dummyHash = encoder.encode("dummy-password");
	private final int sessionHours;
	private final int maxFailures;
	private final int lockMinutes;

	public StaffAuthService(StaffAccountRepository accounts, StaffSessionRepository sessions,
			@Value("${argo.admin.session-hours:8}") int sessionHours,
			@Value("${argo.admin.max-failures:5}") int maxFailures,
			@Value("${argo.admin.lock-minutes:15}") int lockMinutes) {
		this.accounts = accounts;
		this.sessions = sessions;
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
		if (password == null || password.length() < MIN_PASSWORD) {
			throw new IllegalArgumentException("password must be at least " + MIN_PASSWORD + " chars");
		}
		if (accounts.findByUsername(name).isPresent()) {
			throw new IllegalArgumentException("username already exists");
		}
		return accounts.save(new StaffAccount(name, encoder.encode(password), role));
	}

	// 失敗要記錄，不可隨例外回滾
	@Transactional(noRollbackFor = ApiException.class)
	public LoginResponse login(String username, String password) {
		OffsetDateTime now = OffsetDateTime.now();
		Optional<StaffAccount> found = accounts.findByUsername(normalize(username));
		if (found.isEmpty()) {
			encoder.matches(password, dummyHash);
			throw new ApiException(HttpStatus.UNAUTHORIZED, "LOGIN_FAILED");
		}
		StaffAccount staff = found.get();
		if (staff.isLocked(now)) {
			throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "LOGIN_LOCKED");
		}
		boolean ok = encoder.matches(password, staff.getPasswordHash());
		if (!ok || !staff.isEnabled()) {
			if (!ok) {
				staff.recordFailure(maxFailures, now.plusMinutes(lockMinutes));
			}
			throw new ApiException(HttpStatus.UNAUTHORIZED, "LOGIN_FAILED");
		}
		staff.recordSuccess(now);
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
				.filter(StaffAccount::isEnabled);
	}

	@Transactional
	public void logout(String token) {
		if (token != null) {
			sessions.deleteById(hash(token));
		}
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
