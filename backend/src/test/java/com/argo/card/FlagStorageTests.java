package com.argo.card;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.argo.common.Flag;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

// 是否類欄位在資料庫一律存 0 與 1
@SpringBootTest
@Transactional
class FlagStorageTests {

	@Autowired
	CardRepository cards;
	@Autowired
	CardSetRepository sets;
	@Autowired
	JdbcTemplate jdbc;

	@Test
	void flagHelperMapsToZeroAndOne() {
		assertEquals(1, Flag.of(true));
		assertEquals(0, Flag.of(false));
	}

	@Test
	void entityBooleansAreStoredAsSmallint() {
		sets.saveAndFlush(new CardSet("TS-80", "Flag Set", "booster"));
		Card c = new Card("TS-80|A|");
		c.fill(new OptcgCard("TS80-A", "TS80-A", "TS-80", "Flag Set", "A", null, "C", "Red",
				"Character", "1", "1000", null, null, null, null, null, 10.0, 1.0, null),
				new BigDecimal("0.9"));
		c = cards.saveAndFlush(c);
		assertEquals(1, jdbc.queryForObject("select on_sale from card_set where set_id = 'TS-80'", Integer.class));
		assertEquals(0, jdbc.queryForObject("select price_overridden from card where id = ?", Integer.class, c.getId()));
		c.overridePrice(new BigDecimal("5.00"));
		cards.saveAndFlush(c);
		assertEquals(1, jdbc.queryForObject("select price_overridden from card where id = ?", Integer.class, c.getId()));
		CardSet s = sets.findById("TS-80").orElseThrow();
		s.setOnSale(false);
		sets.saveAndFlush(s);
		assertEquals(0, jdbc.queryForObject("select on_sale from card_set where set_id = 'TS-80'", Integer.class));
	}

	@Test
	void databaseRejectsValuesOtherThanZeroAndOne() {
		sets.saveAndFlush(new CardSet("TS-81", "Flag Set", "booster"));
		assertThrows(DataAccessException.class, () -> jdbc.update("update card_set set on_sale = 2 where set_id = 'TS-81'"));
	}

	@Test
	void columnsAreSmallint() {
		for (String[] col : new String[][] { { "card", "price_overridden" }, { "card_set", "on_sale" },
				{ "staff_account", "disabled" }, { "admin_menu", "enabled" }, { "staff_audit_log", "success" },
				{ "ip_block", "auto" }, { "ip_auto_block_rule", "enabled" } }) {
			assertEquals("smallint", jdbc.queryForObject(
					"select data_type from information_schema.columns where table_name = ? and column_name = ?",
					String.class, col[0], col[1]), col[0] + "." + col[1]);
		}
	}
}
