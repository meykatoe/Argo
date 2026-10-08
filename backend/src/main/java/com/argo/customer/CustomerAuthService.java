package com.argo.customer;

import com.argo.common.ApiException;
import com.argo.common.ErrorCode;
import com.argo.common.PasswordPolicy;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomerAuthService {

	// bcrypt 只看前 72 個位元組
	private static final int MAX_PASSWORD_BYTES = 72;

	private final CustomerAccountRepository accounts;
	private final CustomerSessionRepository sessions;
	private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(12);
	private final SecureRandom random = new SecureRandom();
	// 帳號不存在時也做一次比對，避免時間差洩漏
	private final String dummyHash = encoder.encode("dummy-password");
	private final int sessionDays;
	private final int maxFailures;
	private final int lockMinutes;

	public CustomerAuthService(CustomerAccountRepository accounts, CustomerSessionRepository sessions,
			@Value("${argo.customer.session-days:14}") int sessionDays,
			@Value("${argo.customer.max-failures:5}") int maxFailures,
			@Value("${argo.customer.lock-minutes:15}") int lockMinutes) {
		this.accounts = accounts;
		this.sessions = sessions;
		this.sessionDays = sessionDays;
		this.maxFailures = maxFailures;
		this.lockMinutes = lockMinutes;
	}

	// 註冊後直接登入
	@Transactional
	public CustomerAuthView register(String email, String password, String name) {
		String mail = normalize(email);
		if (password.getBytes(StandardCharsets.UTF_8).length > MAX_PASSWORD_BYTES) {
			throw new ApiException(ErrorCode.VALIDATION_ERROR,
					Map.of("password", "validation.passwordTooLong"));
		}
		if (!PasswordPolicy.customerOk(password)) {
			throw new ApiException(ErrorCode.VALIDATION_ERROR,
					Map.of("password", "validation.passwordWeak"));
		}
		if (accounts.findByEmail(mail).isPresent()) {
			throw new ApiException(ErrorCode.EMAIL_TAKEN);
		}
		String display = name == null || name.isBlank() ? null : name.trim();
		CustomerAccount c;
		try {
			c = accounts.saveAndFlush(new CustomerAccount(mail, encoder.encode(password), display));
		} catch (DataIntegrityViolationException e) {
			// 同時註冊同一信箱
			throw new ApiException(ErrorCode.EMAIL_TAKEN);
		}
		c.recordSuccess(OffsetDateTime.now());
		return open(c);
	}

	// 失敗要記錄，不可隨例外回滾
	@Transactional(noRollbackFor = ApiException.class)
	public CustomerAuthView login(String email, String password) {
		OffsetDateTime now = OffsetDateTime.now();
		Optional<CustomerAccount> found = accounts.findByEmailForUpdate(normalize(email));
		if (found.isEmpty()) {
			encoder.matches(password, dummyHash);
			throw new ApiException(ErrorCode.LOGIN_FAILED);
		}
		CustomerAccount c = found.get();
		if (c.isLocked(now)) {
			throw locked(c, now);
		}
		boolean ok = encoder.matches(password, c.getPasswordHash());
		// 超過允許次數就鎖定，這一次也直接告知已鎖定
		if (!ok && c.recordFailure(maxFailures, now.plusMinutes(lockMinutes))) {
			throw locked(c, now);
		}
		if (!ok || c.isDisabled()) {
			throw new ApiException(ErrorCode.LOGIN_FAILED);
		}
		c.recordSuccess(now);
		sessions.deleteExpired(now);
		return open(c);
	}

	// 令牌有效且帳號未停用才回傳顧客
	@Transactional(readOnly = true)
	public Optional<CustomerAccount> authenticate(String token) {
		if (token == null || token.isBlank()) {
			return Optional.empty();
		}
		return sessions.findById(hash(token))
				.filter(s -> s.getExpiresAt().isAfter(OffsetDateTime.now()))
				.flatMap(s -> accounts.findById(s.getCustomerId()))
				.filter(c -> !c.isDisabled());
	}

	@Transactional
	public void logout(String token) {
		if (token != null) {
			sessions.deleteById(hash(token));
		}
	}

	private CustomerAuthView open(CustomerAccount c) {
		String token = newToken();
		OffsetDateTime expires = OffsetDateTime.now().plusDays(sessionDays);
		sessions.save(new CustomerSession(hash(token), c.getId(), expires));
		return new CustomerAuthView(token, c.getEmail(), c.getName(), expires);
	}

	// 回應帶上還要等幾秒，前端可顯示分鐘數
	private static ApiException locked(CustomerAccount c, OffsetDateTime now) {
		return new ApiException(ErrorCode.LOGIN_LOCKED,
				Map.of("retryAfterSeconds", String.valueOf(c.retryAfterSeconds(now))));
	}

	private static String normalize(String email) {
		return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
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
