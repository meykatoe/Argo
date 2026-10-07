package com.argo.card;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.argo.common.ApiException;
import com.argo.common.PageResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

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
				1.5, 1.0, null), new java.math.BigDecimal("0.9"));
		return c;
	}

	private PageResult<CardSummary> find(CardQuery q) {
		return service.search(q, 1, 20, "cardSetId", false);
	}

	@Test
	void filterBySet() {
		assertEquals(2, find(new CardQuery(null, "TS-01", null, null, null, null, false, "en")).total());
	}

	@Test
	void filterByKeywordAndColor() {
		var r = find(new CardQuery("alpha", "TS-01", null, "blue", null, null, false, "en"));
		assertEquals(1, r.total());
		assertEquals("Test Alpha", r.items().get(0).cardName());
	}

	@Test
	void filterByCategory() {
		var r = find(new CardQuery(null, "TS-01", "promo", null, null, null, false, "en"));
		assertEquals(0, r.total());
	}

	@Test
	void detailHasSetName() {
		Long id = find(new CardQuery("beta", null, null, null, null, null, false, "en")).items().get(0).id();
		assertEquals("測試系列", service.get(id, "en").setName());
	}

	@Test
	void badParamsRejected() {
		var q = new CardQuery(null, null, null, null, null, null, false, "en");
		assertThrows(ApiException.class, () -> service.search(q, 0, 20, "cardSetId", false));
		assertThrows(ApiException.class, () -> service.search(q, 1, 20, "hack", false));
		assertTrue(service.listSets("booster", "en").size() > 0);
	}

	@Test
	void batchKeepsOrderAndSkipsMissing() {
		var items = find(new CardQuery(null, "TS-01", null, null, null, null, false, "en")).items();
		Long a = items.get(0).id();
		Long b = items.get(1).id();
		var res = service.getAll(java.util.List.of(b, 999999999L, a, b), "en");
		assertEquals(java.util.List.of(b, a), res.stream().map(CardSummary::id).toList());
	}

	@Test
	void batchRejectsBadSize() {
		assertThrows(ApiException.class, () -> service.getAll(java.util.List.of(), "en"));
		var many = java.util.stream.LongStream.rangeClosed(1, 51).boxed().toList();
		assertThrows(ApiException.class, () -> service.getAll(many, "en"));
	}
}
