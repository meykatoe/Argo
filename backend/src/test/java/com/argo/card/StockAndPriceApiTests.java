package com.argo.card;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class StockAndPriceApiTests {

	@Autowired
	CardService service;
	@Autowired
	CardRepository cards;
	@Autowired
	CardSetRepository sets;
	@Autowired
	JdbcTemplate jdbc;
	@PersistenceContext
	EntityManager em;

	@BeforeEach
	void setUp() {
		sets.save(new CardSet("TS-07", "Test Set", "booster"));
		cards.save(card("A", 10.0));
		cards.save(card("B", 20.0));
		cards.save(card("C", 0.0));
		cards.flush();
	}

	private Card card(String key, double market) {
		Card c = new Card("TS-07|" + key + "|");
		c.fill(new OptcgCard("TS07-" + key, "TS07-" + key, "TS-07", "Test Set", "Card " + key, null,
				"C", "Red", "Character", "1", "1000", null, null, null, null, null,
				market, 1.0, null), new BigDecimal("0.9"));
		return c;
	}

	private CardQuery q(boolean inStock) {
		return new CardQuery(null, "TS-07", null, null, null, null, inStock, "en");
	}

	private void setStock(String key, int stock) {
		jdbc.update("update card set stock = ? where source_key = ?", stock, "TS-07|" + key + "|");
		// 清掉一級快取
		em.clear();
	}

	@Test
	void newCardsHaveNoStock() {
		var items = service.search(q(false), 1, 20, "cardSetId", false).items();
		assertEquals(3, items.size());
		items.forEach(i -> assertEquals(0, i.stock()));
	}

	@Test
	void inStockFilter() {
		setStock("A", 3);
		setStock("C", 5);
		var r = service.search(q(true), 1, 20, "cardSetId", false);
		// C 沒有定價，不算可售
		assertEquals(1, r.total());
		assertEquals("Card A", r.items().get(0).cardName());
		assertEquals(3, r.items().get(0).stock());
	}

	@Test
	void sortBySalePrice() {
		var asc = service.search(q(false), 1, 20, "salePrice", false).items();
		assertEquals("Card C", asc.get(0).cardName());
		var desc = service.search(q(false), 1, 20, "salePrice", true).items();
		assertEquals(new BigDecimal("18.00"), desc.get(0).salePrice());
	}

	@Test
	void detailShowsSalePrice() {
		Long id = service.search(q(false), 1, 20, "salePrice", true).items().get(0).id();
		var d = service.get(id, "en");
		assertEquals(new BigDecimal("18.00"), d.salePrice());
		assertEquals(0, new BigDecimal("20.00").compareTo(d.marketPrice()));
	}

	@Test
	void seedOnlyPricedWithoutStock() {
		setStock("A", 2);
		int changed = cards.seedStock(7);
		assertTrue(changed >= 1);
		cards.flush();
		jdbc.queryForList("select source_key, stock, sale_price from card where source_key like 'TS-07|%'")
				.forEach(r -> {
					String key = (String) r.get("source_key");
					int stock = (Integer) r.get("stock");
					if (key.contains("|A|")) {
						assertEquals(2, stock);
					} else if (key.contains("|B|")) {
						assertEquals(7, stock);
					} else {
						assertEquals(0, stock);
					}
				});
	}
}
