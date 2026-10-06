package com.argo.admin;

import com.argo.card.Card;
import java.math.BigDecimal;

public record AdminCardView(Long id, String cardSetId, String cardName, String rarity,
		String imageUrl, BigDecimal marketPrice, BigDecimal listPrice, BigDecimal extraDiscount,
		BigDecimal salePrice, boolean priceOverridden, int stock) {

	public static AdminCardView from(Card c, BigDecimal rate) {
		return new AdminCardView(c.getId(), c.getCardSetId(), c.getCardName(), c.getRarity(),
				c.getImageUrl(), c.getMarketPrice(), c.listPrice(rate), c.getExtraDiscount(),
				c.getSalePrice(), c.isPriceOverridden(), c.getStock());
	}
}
