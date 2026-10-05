package com.argo.sync;

import com.argo.card.Card;
import com.argo.card.CardRepository;
import com.argo.card.CardSet;
import com.argo.card.CardSetRepository;
import com.argo.card.OptcgCard;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CardSyncService {

	private static final Logger log = LoggerFactory.getLogger(CardSyncService.class);

	private static final Map<String, String> SOURCES = Map.of(
			"booster", "/api/allSetCards/",
			"starter", "/api/allSTCards/",
			"promo", "/api/allPromos/");

	private final OptcgApiClient api;
	private final CardRepository cards;
	private final CardSetRepository sets;

	public CardSyncService(OptcgApiClient api, CardRepository cards, CardSetRepository sets) {
		this.api = api;
		this.cards = cards;
		this.sets = sets;
	}

	public void syncAll() {
		SOURCES.entrySet().stream()
				.sorted(Map.Entry.comparingByKey())
				.forEach(e -> sync(e.getKey(), api.fetch(e.getValue())));
	}

	@Transactional
	public void sync(String category, List<OptcgCard> source) {
		Map<String, OptcgCard> byKey = new LinkedHashMap<>();
		source.forEach(c -> byKey.put(c.sourceKey(), c));

		syncSets(category, byKey.values().stream().toList());

		Map<String, Card> existing = cards.findBySourceKeyIn(byKey.keySet()).stream()
				.collect(Collectors.toMap(Card::getSourceKey, Function.identity()));
		List<Card> toSave = byKey.entrySet().stream().map(e -> {
			Card card = existing.getOrDefault(e.getKey(), new Card(e.getKey()));
			card.fill(e.getValue());
			return card;
		}).toList();
		cards.saveAll(toSave);
		log.info("同步 {} 完成，共 {} 張", category, toSave.size());
	}

	private void syncSets(String category, List<OptcgCard> source) {
		Map<String, CardSet> known = sets.findAll().stream()
				.collect(Collectors.toMap(CardSet::getSetId, Function.identity()));
		Map<String, CardSet> changed = new LinkedHashMap<>();
		for (OptcgCard c : source) {
			CardSet set = known.get(c.setId());
			if (set == null) {
				set = new CardSet(c.setId(), c.setName(), category);
				known.put(c.setId(), set);
			} else {
				set.setSetName(c.setName());
			}
			changed.put(set.getSetId(), set);
		}
		sets.saveAll(changed.values());
	}
}
