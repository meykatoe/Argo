package com.argo.customer;

import com.argo.common.ApiException;
import com.argo.common.ErrorCode;
import com.argo.common.mail.Mailer;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomerEmailService {

	private final CustomerAccountRepository accounts;
	private final CustomerEmailTokenRepository tokens;
	private final PendingRegistrationRepository pending;
	private final Mailer mailer;
	private final String siteUrl;
	private final int verifyHours;
	private final int resendSeconds;
	private final int resetMinutes;
	private final int codeMinutes;
	private final int maxCodeAttempts;

	CustomerEmailService(CustomerAccountRepository accounts, CustomerEmailTokenRepository tokens,
			PendingRegistrationRepository pending, Mailer mailer,
			@Value("${argo.site-url}") String siteUrl,
			@Value("${argo.customer.verify-hours:24}") int verifyHours,
			@Value("${argo.customer.resend-seconds:60}") int resendSeconds,
			@Value("${argo.customer.reset-minutes:60}") int resetMinutes,
			@Value("${argo.customer.code-minutes:10}") int codeMinutes,
			@Value("${argo.customer.max-code-attempts:5}") int maxCodeAttempts) {
		this.accounts = accounts;
		this.tokens = tokens;
		this.pending = pending;
		this.mailer = mailer;
		this.siteUrl = siteUrl.replaceAll("/+$", "");
		this.verifyHours = verifyHours;
		this.resendSeconds = resendSeconds;
		this.resetMinutes = resetMinutes;
		this.codeMinutes = codeMinutes;
		this.maxCodeAttempts = maxCodeAttempts;
	}

	// 暫存註冊資料並寄驗證連結，冷卻中不重寄
	@Transactional
	void startRegistration(String username, String email, String passwordHash, String name) {
		checkCooldown(email);
		pending.deleteByEmail(email);
		sendPending(new PendingRegistration(null, username, email, passwordHash, name, null, null, null));
	}

	// 重寄驗證連結；沒有待驗證資料時靜默結束，不透露信箱狀態
	@Transactional
	void resendRegistration(String email) {
		pending.findFirstByEmailOrderByCreatedAtDesc(email).ifPresent(p -> {
			checkCooldown(email);
			pending.deleteByEmail(email);
			sendPending(p);
		});
	}

	// 驗證通過取出暫存資料並作廢，帳號由呼叫端建立
	@Transactional
	PendingRegistration consumePending(String token) {
		OffsetDateTime now = OffsetDateTime.now();
		String hash = Tokens.hash(token);
		PendingRegistration p = pending.findById(hash).filter(x -> x.getExpiresAt().isAfter(now))
				.orElseThrow(() -> new ApiException(ErrorCode.TOKEN_INVALID));
		pending.deleteByEmail(p.getEmail());
		pending.deleteExpired(now);
		return p;
	}

	private void checkCooldown(String email) {
		OffsetDateTime now = OffsetDateTime.now();
		pending.findFirstByEmailOrderByCreatedAtDesc(email).map(p -> p.getCreatedAt().plusSeconds(resendSeconds))
				.filter(now::isBefore).ifPresent(next -> {
					long wait = Math.max(1, Duration.between(now, next).toSeconds() + 1);
					throw new ApiException(ErrorCode.RESEND_TOO_SOON,
							Map.of("retryAfterSeconds", String.valueOf(wait)));
				});
	}

	private void sendPending(PendingRegistration p) {
		String token = Tokens.create();
		String code = Tokens.code();
		OffsetDateTime now = OffsetDateTime.now();
		pending.save(p.renewed(Tokens.hash(token), now.plusHours(verifyHours), Tokens.codeHash(p.getEmail(), code),
				now.plusMinutes(codeMinutes)));
		mailer.send(p.getEmail(), "Argo 請驗證您的 Email",
				"您好，請用下列任一方式完成 Email 驗證並建立帳號：\n\n"
						+ "方式一：輸入驗證碼 " + code + "（" + codeMinutes + " 分鐘內有效）\n\n"
						+ "方式二：點選連結（" + verifyHours + " 小時內有效）\n" + siteUrl + "/verify-email?token=" + token
						+ "\n\n若不是您本人註冊，請忽略此信，不會建立任何帳號。");
	}

	// 驗證碼錯誤累計達上限就作廢，要重新寄送；失敗紀錄不可隨例外回滾
	@Transactional(noRollbackFor = ApiException.class)
	PendingRegistration consumeCode(String email, String code) {
		OffsetDateTime now = OffsetDateTime.now();
		PendingRegistration p = pending.findLatestForUpdate(email)
				.orElseThrow(() -> new ApiException(ErrorCode.CODE_INVALID));
		boolean ok = p.getCodeExpiresAt().isAfter(now) && p.getExpiresAt().isAfter(now)
				&& Tokens.sameHash(p.getCodeHash(), Tokens.codeHash(email, code));
		if (!ok) {
			if (p.recordCodeFailure() >= maxCodeAttempts) {
				pending.deleteByEmail(email);
			}
			throw new ApiException(ErrorCode.CODE_INVALID);
		}
		pending.deleteByEmail(email);
		pending.deleteExpired(now);
		return p;
	}

	// 不論信箱是否存在都靜默結束，避免被拿來探測；冷卻中也不再寄
	@Transactional
	public void sendReset(String email) {
		String mail = email == null ? "" : email.trim().toLowerCase(java.util.Locale.ROOT);
		accounts.findByEmail(mail).filter(c -> !c.isDisabled()).ifPresent(c -> {
			OffsetDateTime now = OffsetDateTime.now();
			boolean tooSoon = tokens.findFirstByCustomerIdAndPurposeOrderByCreatedAtDesc(c.getId(),
					EmailTokenPurpose.RESET).map(t -> t.getCreatedAt().plusSeconds(resendSeconds)).filter(now::isBefore)
					.isPresent();
			if (tooSoon) {
				return;
			}
			String token = issue(c, EmailTokenPurpose.RESET, Duration.ofMinutes(resetMinutes));
			mailer.send(c.getEmail(), "Argo 重設密碼",
					"您好，請點選下列連結設定新密碼（" + resetMinutes + " 分鐘內有效）：\n" + siteUrl
							+ "/reset-password?token=" + token + "\n\n若不是您本人申請，請忽略此信，密碼不會被更動。");
		});
	}

	// 取出重設令牌對應的帳號並作廢，信中連結能點開代表信箱屬於本人
	@Transactional
	CustomerAccount consumeReset(String token) {
		OffsetDateTime now = OffsetDateTime.now();
		CustomerEmailToken t = tokens.findByTokenHashAndPurpose(Tokens.hash(token), EmailTokenPurpose.RESET)
				.filter(x -> x.getExpiresAt().isAfter(now))
				.orElseThrow(() -> new ApiException(ErrorCode.TOKEN_INVALID));
		CustomerAccount c = accounts.findById(t.getCustomerId())
				.filter(a -> !a.isDisabled())
				.orElseThrow(() -> new ApiException(ErrorCode.TOKEN_INVALID));
		tokens.deleteByCustomer(c.getId(), EmailTokenPurpose.RESET);
		return c;
	}

	private String issue(CustomerAccount c, EmailTokenPurpose purpose, Duration ttl) {
		tokens.deleteByCustomer(c.getId(), purpose);
		String token = Tokens.create();
		tokens.save(new CustomerEmailToken(Tokens.hash(token), c.getId(), purpose, OffsetDateTime.now().plus(ttl)));
		return token;
	}
}
