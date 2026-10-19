package com.argo.order;

import java.net.URLDecoder;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

// 綠界 QueryTradeInfo
@Component
public class EcpayQueryClient implements PaymentQuery {

	private static final Logger log = LoggerFactory.getLogger(EcpayQueryClient.class);

	private final EcpayCheckout ecpay;
	private final String url;
	private final RestClient client;

	public EcpayQueryClient(EcpayCheckout ecpay, @Value("${argo.ecpay.query-url}") String url) {
		this.ecpay = ecpay;
		this.url = url;
		HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
		JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(http);
		factory.setReadTimeout(Duration.ofSeconds(5));
		this.client = RestClient.builder().requestFactory(factory).build();
	}

	@Override
	public Optional<Info> query(String tradeNo) {
		try {
			MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
			ecpay.queryParams(tradeNo, Instant.now().getEpochSecond()).forEach(form::add);
			String body = client.post().uri(url).contentType(MediaType.APPLICATION_FORM_URLENCODED).body(form)
					.retrieve().body(String.class);
			return parse(body);
		} catch (Exception e) {
			log.debug("ecpay query failed trade={} error={}", tradeNo, e.toString());
			return Optional.empty();
		}
	}

	Optional<Info> parse(String body) {
		if (body == null || body.isBlank()) {
			return Optional.empty();
		}
		Map<String, String> p = new LinkedHashMap<>();
		for (String pair : body.trim().split("&")) {
			int i = pair.indexOf('=');
			if (i > 0) {
				p.put(URLDecoder.decode(pair.substring(0, i), StandardCharsets.UTF_8),
						URLDecoder.decode(pair.substring(i + 1), StandardCharsets.UTF_8));
			}
		}
		if (!ecpay.verify(p)) {
			log.debug("ecpay query response has a bad signature");
			return Optional.empty();
		}
		try {
			return Optional.of(new Info(p.getOrDefault("TradeStatus", ""),
					Long.parseLong(p.getOrDefault("TradeAmt", "")), p.get("TradeNo"), p.get("card4no")));
		} catch (NumberFormatException e) {
			return Optional.empty();
		}
	}
}
