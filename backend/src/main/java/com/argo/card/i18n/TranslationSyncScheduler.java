package com.argo.card.i18n;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "argo.translation.scheduled", havingValue = "true")
public class TranslationSyncScheduler {

	private final TranslationSyncService service;

	public TranslationSyncScheduler(TranslationSyncService service) {
		this.service = service;
	}

	@Scheduled(cron = "${argo.translation.cron}")
	public void run() throws InterruptedException {
		service.syncAll();
	}
}
