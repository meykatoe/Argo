package com.argo.card.i18n;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;
import org.junit.jupiter.api.Test;

class TcPageParserTests {

	private static final String CARDS = """
			<dl class="modalCol" id="EB04-061_p3">
			  <dt>
			    <div class="infoCol"><span>EB04-061</span> | <span>SEC</span> | <span>CHARACTER</span></div>
			    <div class="cardName">蒙其・D・魯夫</div>
			  </dt>
			  <dd><div class="backCol">
			    <div class="feature"><h3>特征</h3>蛋頭/四皇/草帽一行人</div>
			    <div class="text"><h3>效果</h3>第一行。<br>【登場時】第二行。</div>
			  </div></dd>
			</dl>
			<dl class="modalCol" id="OP01-001">
			  <dt>
			    <div class="infoCol"><span>OP01-001</span> | <span>L</span></div>
			    <div class="cardName">羅羅亞・索隆</div>
			  </dt>
			  <dd><div class="backCol">
			    <div class="feature"><h3>特征</h3>-</div>
			    <div class="text"><h3>效果</h3>-</div>
			  </div></dd>
			</dl>
			""";

	private static final String INDEX = """
			<select name="series">
			  <option value="554117" selected>補充包 世界最強的戰士【OP-17】</option>
			  <option value="554302" >高級補充包 &lt;br class=&quot;spInline&quot;&gt;ONE PIECE CARD THE BEST vol.2【PRB-02】</option>
			  <option value="554901" >推廣卡</option>
			</select>
			""";

	@Test
	void parseCards() {
		List<TcPageParser.Card> cards = TcPageParser.parseCards(CARDS);
		assertEquals(2, cards.size());
		TcPageParser.Card c = cards.get(0);
		assertEquals("EB04-061", c.cardNumber());
		assertEquals("蒙其・D・魯夫", c.name());
		assertEquals("蛋頭/四皇/草帽一行人", c.feature());
		assertEquals("第一行。\n【登場時】第二行。", c.text());
	}

	@Test
	void dashBecomesNull() {
		TcPageParser.Card c = TcPageParser.parseCards(CARDS).get(1);
		assertNull(c.feature());
		assertNull(c.text());
	}

	@Test
	void parseSeries() {
		List<TcPageParser.Series> list = TcPageParser.parseSeries(INDEX);
		assertEquals(3, list.size());
		assertEquals("OP-17", list.get(0).setId());
		assertEquals("補充包 世界最強的戰士", list.get(0).name());
		assertEquals("PRB-02", list.get(1).setId());
		assertEquals("高級補充包 ONE PIECE CARD THE BEST vol.2", list.get(1).name());
		assertNull(list.get(2).setId());
	}
}
