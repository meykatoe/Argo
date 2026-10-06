package com.argo.card;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class PricingTests {

	private static final BigDecimal RATE = new BigDecimal("0.9");

	private OptcgCard source(double market) {
		return new OptcgCard("TS01-P", "TS01-P", "TS-01", "Test", "Priced", null, "C", "Red",
				"Character", "1", "1000", null, null, null, null, null, market, 1.0, null);
	}

	@Test
	void roundsHalfUp() {
		assertEquals(new BigDecimal("0.64"), Pricing.salePrice(new BigDecimal("0.71"), RATE));
		assertEquals(new BigDecimal("1.22"), Pricing.salePrice(new BigDecimal("1.36"), RATE));
		assertEquals(new BigDecimal("3.15"), Pricing.salePrice(new BigDecimal("3.50"), RATE));
		assertEquals(new BigDecimal("0.00"), Pricing.salePrice(BigDecimal.ZERO, RATE));
	}

	@Test
	void fillComputesSalePrice() {
		Card c = new Card("k");
		c.fill(source(10.00), RATE);
		assertEquals(new BigDecimal("9.00"), c.getSalePrice());
		assertFalse(c.isPriceOverridden());
	}

	@Test
	void resyncFollowsMarketPrice() {
		Card c = new Card("k");
		c.fill(source(10.00), RATE);
		c.fill(source(20.00), RATE);
		assertEquals(new BigDecimal("18.00"), c.getSalePrice());
	}

	@Test
	void overriddenPriceSurvivesSync() {
		Card c = new Card("k");
		c.fill(source(10.00), RATE);
		c.overridePrice(new BigDecimal("12.34"));
		c.fill(source(20.00), RATE);
		assertEquals(new BigDecimal("12.34"), c.getSalePrice());
		assertEquals(0, new BigDecimal("20.00").compareTo(c.getMarketPrice()));
		assertTrue(c.isPriceOverridden());
	}

	@Test
	void extraDiscountDefaultsToOne() {
		Card c = new Card("k");
		assertEquals(0, BigDecimal.ONE.compareTo(c.getExtraDiscount()));
	}

	@Test
	void extraDiscountStacks() {
		Card c = new Card("k");
		c.fill(source(10.00), RATE);
		c.applyExtraDiscount(new BigDecimal("0.4"), RATE);
		assertEquals(new BigDecimal("3.60"), c.getSalePrice());
	}

	@Test
	void extraDiscountSurvivesSync() {
		Card c = new Card("k");
		c.applyExtraDiscount(new BigDecimal("0.4"), RATE);
		c.fill(source(20.00), RATE);
		assertEquals(new BigDecimal("7.20"), c.getSalePrice());
	}

	@Test
	void extraDiscountSkipsOverridden() {
		Card c = new Card("k");
		c.fill(source(10.00), RATE);
		c.overridePrice(new BigDecimal("12.34"));
		c.applyExtraDiscount(new BigDecimal("0.4"), RATE);
		assertEquals(new BigDecimal("12.34"), c.getSalePrice());
	}

	@Test
	void extraDiscountRejectsInvalid() {
		Card c = new Card("k");
		assertThrows(IllegalArgumentException.class, () -> c.applyExtraDiscount(BigDecimal.ZERO, RATE));
		assertThrows(IllegalArgumentException.class, () -> c.applyExtraDiscount(new BigDecimal("1.1"), RATE));
	}
}
