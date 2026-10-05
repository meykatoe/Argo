package com.argo.card;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class OptcgCardTests {

	private OptcgCard card(String image) {
		return new OptcgCard("OP01-001", "OP01-001", "OP-01", "Romance Dawn", "Zoro",
				null, "L", "Red", "Leader", null, "5000", null, "5", null, null,
				image, 1.0, 1.0, "10/11/2025");
	}

	@Test
	void keyUsesImageFile() {
		OptcgCard c = card("https://x.com/media/OP01-001_ab.jpg");
		assertEquals("OP-01|OP01-001|OP01-001_ab.jpg", c.sourceKey());
	}

	@Test
	void keyWithoutImage() {
		assertEquals("OP-01|OP01-001|", card(null).sourceKey());
	}

	@Test
	void badDateIsNull() {
		assertNull(card(null).scrapedDate());
	}
}
