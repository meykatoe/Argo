package com.argo.card.sync;

import com.argo.card.OptcgCard;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class OptcgApiClient {

	private final RestClient client;

	public OptcgApiClient(@Value("${argo.optcg.base-url}") String baseUrl) {
		this.client = RestClient.builder().baseUrl(baseUrl).build();
	}

	public List<OptcgCard> fetch(String path) {
		OptcgCard[] body = client.get().uri(path).retrieve().body(OptcgCard[].class);
		return body == null ? List.of() : List.of(body);
	}
}
