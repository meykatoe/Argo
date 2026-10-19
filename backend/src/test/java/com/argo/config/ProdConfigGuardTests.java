package com.argo.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class ProdConfigGuardTests {

	private MockEnvironment valid() {
		return new MockEnvironment()
				.withProperty("argo.payment.mode", "ecpay")
				.withProperty("spring.datasource.password", "S3cret-pass")
				.withProperty("argo.ecpay.merchant-id", "2000132")
				.withProperty("argo.ecpay.hash-key", "realKey")
				.withProperty("argo.ecpay.hash-iv", "realIv")
				.withProperty("argo.ecpay.checkout-url", "https://payment.ecpay.com.tw/Cashier/AioCheckOut/V5")
				.withProperty("argo.ecpay.query-url", "https://payment.ecpay.com.tw/Cashier/QueryTradeInfo/V5")
				.withProperty("argo.site-url", "https://shop.example.com")
				.withProperty("argo.payment.api-url", "https://api.example.com")
				.withProperty("argo.cors.origins", "https://shop.example.com,https://admin.example.com");
	}

	@Test
	void validPasses() {
		assertThat(ProdConfigGuard.check(valid())).isEmpty();
	}

	@Test
	void devDefaultsRejected() {
		MockEnvironment dev = new MockEnvironment()
				.withProperty("argo.payment.mode", "fake")
				.withProperty("spring.datasource.password", "1234")
				.withProperty("argo.ecpay.merchant-id", "3002607")
				.withProperty("argo.ecpay.hash-key", "pwFHCqoQZGmho4w6")
				.withProperty("argo.ecpay.hash-iv", "EkRm7iFT261dpevs")
				.withProperty("argo.ecpay.checkout-url", "https://payment-stage.ecpay.com.tw/x")
				.withProperty("argo.ecpay.query-url", "https://payment-stage.ecpay.com.tw/q")
				.withProperty("argo.site-url", "http://localhost:5173")
				.withProperty("argo.payment.api-url", "http://localhost:8080")
				.withProperty("argo.cors.origins", "http://localhost:5173");
		assertThat(ProdConfigGuard.check(dev)).hasSize(10);
	}

	@Test
	void fakeModeRejected() {
		assertThat(ProdConfigGuard.check(valid().withProperty("argo.payment.mode", "fake")))
				.hasSize(1);
	}

	@Test
	void missingValuesRejected() {
		assertThat(ProdConfigGuard.check(new MockEnvironment())).isNotEmpty();
	}

	@Test
	void guardBlocksStartup() {
		assertThrows(IllegalStateException.class, () -> new ProdConfigGuard(new MockEnvironment()));
	}
}
