package com.argo.order;

import static com.argo.order.OrderTestSupport.card;
import static com.argo.order.OrderTestSupport.request;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.argo.card.Card;
import com.argo.card.CardQuery;
import com.argo.card.CardRepository;
import com.argo.card.CardService;
import com.argo.card.CardSetRepository;
import com.argo.common.ApiException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class OrderShelfTests {

	@Autowired
	OrderService orders;
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

	Card a;

	@BeforeEach
	void setUp() {
		a = card(cards, sets, jdbc, "TS-60", "A", 10.0, 5);
		em.clear();
	}

	private void shelf(boolean onSale) {
		jdbc.update("update card_set set on_sale = ? where set_id = 'TS-60'", onSale ? 1 : 0);
		em.clear();
	}

	private CardQuery q(boolean inStock) {
		return new CardQuery(null, "TS-60", null, null, null, null, inStock, "en");
	}

	@Test
	void newSeriesIsOnSale() {
		var item = service.search(q(false), 1, 20, "cardSetId", false).items().get(0);
		assertEquals(1, item.onSale());
		assertEquals(1, service.get(a.getId(), "en").onSale());
	}

	@Test
	void offShelfCardsStayVisibleButFlagged() {
		shelf(false);
		var items = service.search(q(false), 1, 20, "cardSetId", false).items();
		assertEquals(1, items.size());
		assertEquals(0, items.get(0).onSale());
		assertEquals(0, service.get(a.getId(), "en").onSale());
		assertEquals(0, service.getAll(java.util.List.of(a.getId()), "en").get(0).onSale());
	}

	@Test
	void inStockFilterExcludesOffShelf() {
		assertEquals(1, service.search(q(true), 1, 20, "cardSetId", false).items().size());
		shelf(false);
		assertEquals(0, service.search(q(true), 1, 20, "cardSetId", false).items().size());
	}

	@Test
	void orderIsRejectedAndStockUntouched() {
		shelf(false);
		ApiException e = assertThrows(ApiException.class, () -> orders.create(request(a.getId(), 1L), "en", null));
		assertEquals("ITEM_UNAVAILABLE", e.getCode());
		assertEquals(5, jdbc.queryForObject("select stock from card where id = ?", Integer.class, a.getId()));
	}

	@Test
	void stockGuardAlsoBlocksOffShelf() {
		shelf(false);
		assertEquals(0, cards.decrementStock(a.getId(), 1));
		shelf(true);
		assertEquals(1, cards.decrementStock(a.getId(), 1));
	}

	@Test
	void backOnShelfCanBeBoughtAgain() {
		shelf(false);
		assertThrows(ApiException.class, () -> orders.create(request(a.getId(), 1L), "en", null));
		shelf(true);
		assertEquals(1, orders.create(request(a.getId(), 1L), "en", null).items().size());
	}

	@Test
	void setListShowsShelfState() {
		shelf(false);
		var set = service.listSets(null, "en").stream().filter(s -> s.setId().equals("TS-60")).findFirst().orElseThrow();
		assertEquals(0, set.onSale());
	}
}
