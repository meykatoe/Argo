package com.argo.order;

import java.math.BigDecimal;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

// 公開匯率 API，免金鑰
@Component
public class OpenErRateSource implements RateSource {

	record Response(String result, Map<String, BigDecimal> rates) {
	}

	private final RestClient client;

	public OpenErRateSource(@Value("${argo.fx.base-url}") String baseUrl) {
		// 逾時要短，避免拖慢下單
		HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
		JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(http);
		factory.setReadTimeout(Duration.ofSeconds(3));
		this.client = RestClient.builder().baseUrl(baseUrl).requestFactory(factory).build();
	}

	@Override
	public BigDecimal usdToTwd() {
		Response body = client.get().uri("/v6/latest/USD").retrieve().body(Response.class);
		if (body == null || !"success".equals(body.result()) || body.rates() == null) {
			throw new IllegalStateException("fx response invalid");
		}
		BigDecimal rate = body.rates().get("TWD");
		if (rate == null) {
			throw new IllegalStateException("fx rate missing");
		}
		return rate;
	}
}
