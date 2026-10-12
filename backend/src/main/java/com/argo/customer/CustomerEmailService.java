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
	private final Mailer mailer;
	private final String siteUrl;
	private final int verifyHours;
	private final int resendSeconds;
	private final int resetMinutes;

	CustomerEmailService(CustomerAccountRepository accounts, CustomerEmailTokenRepository tokens, Mailer mailer,
			@Value("${argo.site-url}") String siteUrl,
			@Value("${argo.customer.verify-hours:24}") int verifyHours,
			@Value("${argo.customer.resend-seconds:60}") int resendSeconds,
			@Value("${argo.customer.reset-minutes:60}") int resetMinutes) {
		this.accounts = accounts;
		this.tokens = tokens;
		this.mailer = mailer;
		this.siteUrl = siteUrl.replaceAll("/+$", "");
		this.verifyHours = verifyHours;
		this.resendSeconds = resendSeconds;
		this.resetMinutes = resetMinutes;
	}

	// 註冊時呼叫，同一交易內
	@Transactional
	public void sendVerification(CustomerAccount c) {
		if (c.isEmailVerified()) {
			return;
		}
		String token = issue(c, EmailTokenPurpose.VERIFY, Duration.ofHours(verifyHours));
		mailer.send(c.getEmail(), "Argo 請驗證您的 Email",
				"您好，請點選下列連結完成 Email 驗證（" + verifyHours + " 小時內有效）：\n" + siteUrl
						+ "/verify-email?token=" + token + "\n\n若不是您本人註冊，請忽略此信。");
	}

	// 重寄有冷卻時間
	@Transactional
	public void resendVerification(CustomerAccount c) {
		if (c.isEmailVerified()) {
			return;
		}
		OffsetDateTime now = OffsetDateTime.now();
		tokens.findFirstByCustomerIdAndPurposeOrderByCreatedAtDesc(c.getId(), EmailTokenPurpose.VERIFY)
				.map(t -> t.getCreatedAt().plusSeconds(resendSeconds))
				.filter(now::isBefore)
				.ifPresent(next -> {
					long wait = Math.max(1, Duration.between(now, next).toSeconds() + 1);
					throw new ApiException(ErrorCode.RESEND_TOO_SOON,
							Map.of("retryAfterSeconds", String.valueOf(wait)));
				});
		sendVerification(c);
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
		c.verifyEmail(now);
		return c;
	}

	// 令牌只能用一次
	@Transactional
	public void verify(String token) {
		OffsetDateTime now = OffsetDateTime.now();
		CustomerEmailToken t = tokens.findByTokenHashAndPurpose(Tokens.hash(token), EmailTokenPurpose.VERIFY)
				.filter(x -> x.getExpiresAt().isAfter(now))
				.orElseThrow(() -> new ApiException(ErrorCode.TOKEN_INVALID));
		CustomerAccount c = accounts.findById(t.getCustomerId())
				.orElseThrow(() -> new ApiException(ErrorCode.TOKEN_INVALID));
		c.verifyEmail(now);
		tokens.deleteByCustomer(c.getId(), EmailTokenPurpose.VERIFY);
		tokens.deleteExpired(now);
	}

	private String issue(CustomerAccount c, EmailTokenPurpose purpose, Duration ttl) {
		tokens.deleteByCustomer(c.getId(), purpose);
		String token = Tokens.create();
		tokens.save(new CustomerEmailToken(Tokens.hash(token), c.getId(), purpose, OffsetDateTime.now().plus(ttl)));
		return token;
	}
}
