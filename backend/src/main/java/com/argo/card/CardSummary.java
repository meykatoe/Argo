package com.argo.card;

import java.math.BigDecimal;

public record CardSummary(Long id, String cardSetId, String cardImageId, String setId,
		String cardName, String rarity, String cardColor, String cardType, String cardCost,
		String cardPower, String imageUrl, BigDecimal marketPrice) {

	public static CardSummary from(Card c) {
		return new CardSummary(c.getId(), c.getCardSetId(), c.getCardImageId(), c.getSetId(),
				c.getCardName(), c.getRarity(), c.getCardColor(), c.getCardType(), c.getCardCost(),
				c.getCardPower(), c.getImageUrl(), c.getMarketPrice());
	}
}
