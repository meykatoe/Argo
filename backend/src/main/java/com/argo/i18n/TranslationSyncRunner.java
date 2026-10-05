package com.argo.i18n;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "argo.translation.on-startup", havingValue = "true")
public class TranslationSyncRunner implements ApplicationRunner {

	private final TranslationSyncService service;

	public TranslationSyncRunner(TranslationSyncService service) {
		this.service = service;
	}

	@Override
	public void run(ApplicationArguments args) throws Exception {
		service.syncAll();
	}
}
