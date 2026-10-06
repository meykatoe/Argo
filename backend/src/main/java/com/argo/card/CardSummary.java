package com.argo.card;

import com.argo.i18n.CardTranslation;
import java.math.BigDecimal;

public record CardSummary(Long id, String cardSetId, String cardImageId, String setId,
		String cardName, String cardNameEn, String rarity, String cardColor, String cardType,
		String cardCost, String cardPower, String imageUrl, BigDecimal marketPrice,
		BigDecimal salePrice, BigDecimal listPrice, int stock, boolean onSale) {

	// 翻譯可為空，空則用原文
	public static CardSummary from(Card c, CardTranslation tr, BigDecimal rate,
			boolean onSale) {
		String name = tr != null ? tr.getCardName() : c.getCardName();
		return new CardSummary(c.getId(), c.getCardSetId(), c.getCardImageId(), c.getSetId(),
				name, c.getCardName(), c.getRarity(), c.getCardColor(), c.getCardType(),
				c.getCardCost(), c.getCardPower(), c.getImageUrl(), c.getMarketPrice(),
				c.getSalePrice(), c.listPrice(rate), c.getStock(), onSale);
	}
}
