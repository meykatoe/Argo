package com.argo.card;

import com.argo.i18n.CardTranslation;
import java.math.BigDecimal;

public record CardDetail(Long id, String cardSetId, String cardImageId, String setId,
		String setName, String cardName, String cardNameEn, String cardText, String rarity,
		String cardColor, String cardType, String cardCost, String cardPower,
		Integer counterAmount, String life, String attribute, String subTypes, String imageUrl,
		BigDecimal marketPrice) {

	// 翻譯欄位為空則用原文
	public static CardDetail from(Card c, String setName, CardTranslation tr) {
		String name = tr != null ? tr.getCardName() : c.getCardName();
		String text = tr != null && tr.getCardText() != null ? tr.getCardText() : c.getCardText();
		String types = tr != null && tr.getSubTypes() != null ? tr.getSubTypes() : c.getSubTypes();
		return new CardDetail(c.getId(), c.getCardSetId(), c.getCardImageId(), c.getSetId(),
				setName, name, c.getCardName(), text, c.getRarity(), c.getCardColor(),
				c.getCardType(), c.getCardCost(), c.getCardPower(), c.getCounterAmount(),
				c.getLife(), c.getAttribute(), types, c.getImageUrl(), c.getMarketPrice());
	}
}
