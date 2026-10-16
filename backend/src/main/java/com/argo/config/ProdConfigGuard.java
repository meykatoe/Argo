package com.argo.config;

import java.util.ArrayList;
import java.util.List;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;

// 正式環境設定檢查
@Configuration
@Profile("prod")
public class ProdConfigGuard {

	private static final String STAGE_MERCHANT = "3002607";
	private static final String STAGE_HASH_KEY = "pwFHCqoQZGmho4w6";
	private static final String STAGE_HASH_IV = "EkRm7iFT261dpevs";
	private static final String DEV_DB_PASSWORD = "1234";

	public ProdConfigGuard(Environment env) {
		List<String> problems = check(env);
		if (!problems.isEmpty()) {
			throw new IllegalStateException(
					"prod 設定不安全，拒絕啟動：\n- " + String.join("\n- ", problems));
		}
	}

	static List<String> check(Environment env) {
		List<String> problems = new ArrayList<>();
		if (!"ecpay".equalsIgnoreCase(env.getProperty("argo.payment.mode"))) {
			problems.add("ARGO_PAYMENT_MODE 必須是 ecpay");
		}
		String dbPassword = env.getProperty("spring.datasource.password", "");
		if (dbPassword.isBlank() || DEV_DB_PASSWORD.equals(dbPassword)) {
			problems.add("DB_PASSWORD 不可為空或開發預設值");
		}
		if (STAGE_MERCHANT.equals(env.getProperty("argo.ecpay.merchant-id"))) {
			problems.add("ARGO_ECPAY_MERCHANT_ID 不可用測試商店");
		}
		if (STAGE_HASH_KEY.equals(env.getProperty("argo.ecpay.hash-key"))) {
			problems.add("ARGO_ECPAY_HASH_KEY 不可用測試值");
		}
		if (STAGE_HASH_IV.equals(env.getProperty("argo.ecpay.hash-iv"))) {
			problems.add("ARGO_ECPAY_HASH_IV 不可用測試值");
		}
		if (env.getProperty("argo.ecpay.checkout-url", "").contains("stage")) {
			problems.add("ARGO_ECPAY_CHECKOUT_URL 不可用測試網址");
		}
		for (String key : List.of("argo.site-url", "argo.payment.api-url", "argo.cors.origins")) {
			if (isLocal(env.getProperty(key, ""))) {
				problems.add(key + " 不可指向本機");
			}
		}
		return problems;
	}

	private static boolean isLocal(String value) {
		return value.isBlank() || value.contains("localhost") || value.contains("127.0.0.1");
	}
}
