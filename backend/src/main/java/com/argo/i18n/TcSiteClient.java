package com.argo.i18n;

import java.net.http.HttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class TcSiteClient {

	private final RestClient client;

	public TcSiteClient(@Value("${argo.tc.base-url}") String baseUrl) {
		// 官方站會轉址
		HttpClient http = HttpClient.newBuilder()
				.followRedirects(HttpClient.Redirect.NORMAL)
				.build();
		this.client = RestClient.builder()
				.requestFactory(new JdkClientHttpRequestFactory(http))
				.baseUrl(baseUrl)
				.defaultHeader("User-Agent", "ArgoShop/1.0")
				.build();
	}

	public String fetchIndex() {
		return require(client.get().uri("/cardlist/").retrieve().body(String.class));
	}

	public String fetchSeries(String siteId) {
		return require(client.get().uri("/cardlist/?series={id}", siteId).retrieve().body(String.class));
	}

	private static String require(String body) {
		if (body == null || body.isBlank()) {
			throw new IllegalStateException("官方站回應為空");
		}
		return body;
	}
}
