package com.argo.card.sync;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "argo.sync.scheduled", havingValue = "true")
public class CardSyncScheduler {

	private final CardSyncService service;

	public CardSyncScheduler(CardSyncService service) {
		this.service = service;
	}

	@Scheduled(cron = "${argo.sync.cron:0 0 4 * * *}")
	public void run() {
		service.syncAll();
	}
}
