package com.argo.card;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

// 僅開發用，預設不啟用
@Component
@ConditionalOnProperty(name = "argo.dev.seed-stock")
public class DevStockSeeder implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(DevStockSeeder.class);

	private final CardRepository cards;
	private final int amount;

	public DevStockSeeder(CardRepository cards, @Value("${argo.dev.seed-stock}") int amount) {
		this.cards = cards;
		this.amount = amount;
	}

	@Override
	public void run(ApplicationArguments args) {
		log.info("開發用庫存補齊，共 {} 張", cards.seedStock(amount));
	}
}
