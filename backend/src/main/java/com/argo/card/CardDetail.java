package com.argo.card;

import java.math.BigDecimal;

public record CardDetail(Long id, String cardSetId, String cardImageId, String setId,
		String setName, String cardName, String cardText, String rarity, String cardColor,
		String cardType, String cardCost, String cardPower, Integer counterAmount, String life,
		String attribute, String subTypes, String imageUrl, BigDecimal marketPrice) {

	public static CardDetail from(Card c, String setName) {
		return new CardDetail(c.getId(), c.getCardSetId(), c.getCardImageId(), c.getSetId(),
				setName, c.getCardName(), c.getCardText(), c.getRarity(), c.getCardColor(),
				c.getCardType(), c.getCardCost(), c.getCardPower(), c.getCounterAmount(),
				c.getLife(), c.getAttribute(), c.getSubTypes(), c.getImageUrl(), c.getMarketPrice());
	}
}
