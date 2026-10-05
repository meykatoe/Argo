package com.argo.i18n;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.argo.card.Card;
import com.argo.card.CardQuery;
import com.argo.card.CardRepository;
import com.argo.card.CardService;
import com.argo.card.CardSet;
import com.argo.card.CardSetRepository;
import com.argo.card.OptcgCard;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class TranslationApiTests {

	@Autowired
	CardService service;
	@Autowired
	CardRepository cards;
	@Autowired
	CardSetRepository sets;
	@Autowired
	TranslationSyncService sync;

	@BeforeEach
	void setUp() {
		sets.save(new CardSet("TS-09", "Test Set", "booster"));
		cards.save(card("A", "Test Luffy"));
		cards.save(card("B", "Test Nami"));
		cards.flush();
		// 只翻譯其中一張
		sync.saveCards(java.util.List.of(
				new TcPageParser.Card("TS09-A", "測試魯夫", "草帽一行人", "效果文字")));
		sync.saveSets(java.util.List.of(new TcPageParser.Series("1", "TS-09", "測試系列")));
	}

	private Card card(String key, String name) {
		Card c = new Card("TS-09|" + key + "|");
		c.fill(new OptcgCard("TS09-" + key, "TS09-" + key, "TS-09", "Test Set", name, "effect",
				"C", "Red", "Character", "1", "1000", null, null, null, "Pirates", null,
				1.0, 1.0, null));
		return c;
	}

	private CardQuery q(String keyword, String lang) {
		return new CardQuery(keyword, "TS-09", null, null, null, null, lang);
	}

	@Test
	void translatedNameInZh() {
		var items = service.search(q(null, "zh-TW"), 1, 20, "cardSetId", false).items();
		assertEquals("測試魯夫", items.get(0).cardName());
		assertEquals("Test Luffy", items.get(0).cardNameEn());
	}

	@Test
	void fallbackToEnglish() {
		var items = service.search(q(null, "zh-TW"), 1, 20, "cardSetId", false).items();
		assertEquals("Test Nami", items.get(1).cardName());
	}

	@Test
	void englishIgnoresTranslation() {
		var items = service.search(q(null, "en"), 1, 20, "cardSetId", false).items();
		assertEquals("Test Luffy", items.get(0).cardName());
	}

	@Test
	void unknownLangIsEnglish() {
		var items = service.search(q(null, "fr"), 1, 20, "cardSetId", false).items();
		assertEquals("Test Luffy", items.get(0).cardName());
	}

	@Test
	void searchByChineseName() {
		assertEquals(1, service.search(q("魯夫", "zh-TW"), 1, 20, "cardSetId", false).total());
		assertEquals(0, service.search(q("魯夫", "en"), 1, 20, "cardSetId", false).total());
	}

	@Test
	void detailTranslated() {
		Long id = service.search(q("魯夫", "zh-TW"), 1, 20, "cardSetId", false).items().get(0).id();
		var d = service.get(id, "zh-TW");
		assertEquals("測試魯夫", d.cardName());
		assertEquals("效果文字", d.cardText());
		assertEquals("草帽一行人", d.subTypes());
		assertEquals("測試系列", d.setName());
		assertEquals("effect", service.get(id, "en").cardText());
	}

	@Test
	void detailFallbackKeepsEnglishText() {
		Long id = service.search(q("nami", "zh-TW"), 1, 20, "cardSetId", false).items().get(0).id();
		var d = service.get(id, "zh-TW");
		assertEquals("Test Nami", d.cardName());
		assertEquals("effect", d.cardText());
	}

	@Test
	void setNamesLocalized() {
		var zh = service.listSets("booster", "zh-TW").stream()
				.filter(s -> s.setId().equals("TS-09")).findFirst().orElseThrow();
		assertEquals("測試系列", zh.setName());
		assertEquals("Test Set", zh.setNameEn());
	}
}
