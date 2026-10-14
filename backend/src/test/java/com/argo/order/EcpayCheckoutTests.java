package com.argo.order;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class EcpayCheckoutTests {

	private final EcpayCheckout ecpay = new EcpayCheckout("3002607", "pwFHCqoQZGmho4w6", "EkRm7iFT261dpevs",
			"https://payment-stage.ecpay.com.tw/Cashier/AioCheckOut/V5");

	private EcpayCheckout.Form form() {
		return ecpay.build("AR1234567890", 321, "Argo AR261005-K7P3QX", "https://api.test/notify",
				"https://api.test/result", "https://site.test/back",
				ZonedDateTime.of(2026, 10, 14, 9, 30, 0, 0, ZoneId.of("Asia/Taipei")));
	}

	@Test
	void buildsRequiredFields() {
		EcpayCheckout.Form f = form();
		assertThat(f.action()).contains("AioCheckOut");
		assertThat(f.fields()).containsEntry("MerchantID", "3002607")
				.containsEntry("TotalAmount", "321")
				.containsEntry("ChoosePayment", "Credit")
				.containsEntry("MerchantTradeDate", "2026/10/14 09:30:00");
		assertThat(f.fields().get("CheckMacValue")).matches("[0-9A-F]{64}");
	}

	@Test
	void macIsStableAndVerifies() {
		Map<String, String> fields = new LinkedHashMap<>(form().fields());
		assertThat(fields.get("CheckMacValue")).isEqualTo(form().fields().get("CheckMacValue"));
		assertThat(ecpay.verify(fields)).isTrue();
	}

	@Test
	void tamperedOrMissingMacFails() {
		Map<String, String> fields = new LinkedHashMap<>(form().fields());
		fields.put("TotalAmount", "1");
		assertThat(ecpay.verify(fields)).isFalse();
		fields.remove("CheckMacValue");
		assertThat(ecpay.verify(fields)).isFalse();
	}

	@Test
	void wrongKeyFails() {
		EcpayCheckout other = new EcpayCheckout("3002607", "x", "y", "u");
		assertThat(other.verify(form().fields())).isFalse();
	}
}
