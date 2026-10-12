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

	CustomerEmailService(CustomerAccountRepository accounts, CustomerEmailTokenRepository tokens, Mailer mailer,
			@Value("${argo.site-url}") String siteUrl,
			@Value("${argo.customer.verify-hours:24}") int verifyHours,
			@Value("${argo.customer.resend-seconds:60}") int resendSeconds) {
		this.accounts = accounts;
		this.tokens = tokens;
		this.mailer = mailer;
		this.siteUrl = siteUrl.replaceAll("/+$", "");
		this.verifyHours = verifyHours;
		this.resendSeconds = resendSeconds;
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
