package com.argo.card;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.argo.common.PageResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@SpringBootTest
@Transactional
class CardServiceTests {

	@Autowired
	CardService service;
	@Autowired
	CardRepository cards;
	@Autowired
	CardSetRepository sets;

	@BeforeEach
	void setUp() {
		sets.save(new CardSet("TS-01", "測試系列", "booster"));
		cards.save(card("T1", "Test Alpha", "Red Blue", "SR"));
		cards.save(card("T2", "Test Beta", "Green", "C"));
		cards.flush();
	}

	private Card card(String key, String name, String color, String rarity) {
		Card c = new Card("TS-01|" + key + "|");
		c.fill(new OptcgCard("TS01-" + key, "TS01-" + key, "TS-01", "測試系列", name, null,
				rarity, color, "Character", "1", "1000", null, null, null, null, null,
				1.5, 1.0, null));
		return c;
	}

	private PageResult<CardSummary> find(CardQuery q) {
		return service.search(q, 1, 20, "cardSetId", false);
	}

	@Test
	void filterBySet() {
		assertEquals(2, find(new CardQuery(null, "TS-01", null, null, null, null, "en")).total());
	}

	@Test
	void filterByKeywordAndColor() {
		var r = find(new CardQuery("alpha", "TS-01", null, "blue", null, null, "en"));
		assertEquals(1, r.total());
		assertEquals("Test Alpha", r.items().get(0).cardName());
	}

	@Test
	void filterByCategory() {
		var r = find(new CardQuery(null, "TS-01", "promo", null, null, null, "en"));
		assertEquals(0, r.total());
	}

	@Test
	void detailHasSetName() {
		Long id = find(new CardQuery("beta", null, null, null, null, null, "en")).items().get(0).id();
		assertEquals("測試系列", service.get(id, "en").setName());
	}

	@Test
	void badParamsRejected() {
		var q = new CardQuery(null, null, null, null, null, null, "en");
		assertThrows(ResponseStatusException.class, () -> service.search(q, 0, 20, "cardSetId", false));
		assertThrows(ResponseStatusException.class, () -> service.search(q, 1, 20, "hack", false));
		assertTrue(service.listSets("booster", "en").size() > 0);
	}
}
