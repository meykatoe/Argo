package com.argo.i18n;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TranslationSyncService {

	private static final Logger log = LoggerFactory.getLogger(TranslationSyncService.class);

	private final TcSiteClient client;
	private final CardTranslationRepository cards;
	private final CardSetTranslationRepository sets;
	private final long delayMs;

	public TranslationSyncService(TcSiteClient client, CardTranslationRepository cards,
			CardSetTranslationRepository sets, @Value("${argo.tc.delay-ms}") long delayMs) {
		this.client = client;
		this.cards = cards;
		this.sets = sets;
		this.delayMs = delayMs;
	}

	public void syncAll() throws InterruptedException {
		List<TcPageParser.Series> series = TcPageParser.parseSeries(client.fetchIndex());
		Map<String, TcPageParser.Card> all = new LinkedHashMap<>();
		for (TcPageParser.Series s : series) {
			// 間隔請求避免打擾對方
			Thread.sleep(delayMs);
			try {
				TcPageParser.parseCards(client.fetchSeries(s.siteId()))
						.forEach(c -> all.putIfAbsent(c.cardNumber(), c));
			} catch (RuntimeException e) {
				log.warn("系列 {} 抓取失敗：{}", s.siteId(), e.getMessage());
			}
		}
		saveSets(series);
		saveCards(all.values().stream().toList());
	}

	@Transactional
	public void saveSets(List<TcPageParser.Series> series) {
		Map<String, CardSetTranslation> known = sets.findByLocale(Locales.ZH_TW).stream()
				.collect(Collectors.toMap(CardSetTranslation::getSetId, Function.identity()));
		Map<String, CardSetTranslation> changed = new HashMap<>();
		for (TcPageParser.Series s : series) {
			if (s.setId() == null) {
				continue;
			}
			CardSetTranslation t = known.get(s.setId());
			if (t == null) {
				t = new CardSetTranslation(s.setId(), Locales.ZH_TW, s.name());
			} else {
				t.setSetName(s.name());
			}
			changed.put(s.setId(), t);
		}
		sets.saveAll(changed.values());
		log.info("系列翻譯完成，共 {} 筆", changed.size());
	}

	@Transactional
	public void saveCards(List<TcPageParser.Card> parsed) {
		Map<String, CardTranslation> known = cards.findByLocale(Locales.ZH_TW).stream()
				.collect(Collectors.toMap(CardTranslation::getCardSetId, Function.identity()));
		List<CardTranslation> toSave = parsed.stream().map(c -> {
			CardTranslation t = known.getOrDefault(c.cardNumber(),
					new CardTranslation(c.cardNumber(), Locales.ZH_TW));
			t.fill(c.name(), c.text(), c.feature());
			return t;
		}).toList();
		cards.saveAll(toSave);
		log.info("卡片翻譯完成，共 {} 筆", toSave.size());
	}
}
