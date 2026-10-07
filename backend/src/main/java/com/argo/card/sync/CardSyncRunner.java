package com.argo.card.sync;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "argo.sync.on-startup", havingValue = "true")
public class CardSyncRunner implements ApplicationRunner {

	private final CardSyncService service;

	public CardSyncRunner(CardSyncService service) {
		this.service = service;
	}

	@Override
	public void run(ApplicationArguments args) {
		service.syncAll();
	}
}
