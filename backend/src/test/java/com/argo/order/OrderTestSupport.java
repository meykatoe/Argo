package com.argo.order;

import com.argo.card.Card;
import com.argo.card.CardRepository;
import com.argo.card.CardSet;
import com.argo.card.CardSetRepository;
import com.argo.card.OptcgCard;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;

// 測試共用的資料準備
final class OrderTestSupport {

	static final String EMAIL = "buyer@test.local";

	private OrderTestSupport() {
	}

	static Card card(CardRepository cards, CardSetRepository sets, JdbcTemplate jdbc, String setId,
			String key, double market, int stock) {
		if (!sets.existsById(setId)) {
			sets.save(new CardSet(setId, "Test " + setId, "booster"));
		}
		Card c = new Card(setId + "|" + key + "|");
		c.fill(new OptcgCard(setId.replace("-", "") + "-" + key, setId.replace("-", "") + "-" + key,
				setId, "Test", "Card " + key, "effect", "C", "Red", "Character", "1", "1000", null,
				null, null, null, "http://img/" + key + ".jpg", market, 1.0, null), new BigDecimal("0.9"));
		c = cards.saveAndFlush(c);
		jdbc.update("update card set stock = ? where id = ?", stock, c.getId());
		return c;
	}

	static CreateOrderRequest request(Long... cardIdAndQty) {
		var items = new java.util.ArrayList<CreateOrderRequest.Item>();
		for (int i = 0; i < cardIdAndQty.length; i += 2) {
			items.add(new CreateOrderRequest.Item(cardIdAndQty[i], cardIdAndQty[i + 1].intValue()));
		}
		return new CreateOrderRequest(List.copyOf(items),
				new CreateOrderRequest.Customer("王小明", "Buyer@Test.local", "0912345678"),
				new CreateOrderRequest.Shipping("王小明", "0912345678", "100", "台北市", "中正區重慶南路一段 122 號"));
	}

	static PayRequest pay(String number) {
		return new PayRequest(EMAIL, new PayRequest.Card(number, 12, 2099, "123", "WANG"));
	}
}
