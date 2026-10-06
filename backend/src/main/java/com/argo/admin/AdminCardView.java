package com.argo.admin;

import com.argo.card.Card;
import com.argo.i18n.CardTranslation;
import java.math.BigDecimal;

public record AdminCardView(Long id, String cardSetId, String cardName, String cardNameEn,
		String rarity, String imageUrl, BigDecimal marketPrice, BigDecimal listPrice,
		BigDecimal extraDiscount, BigDecimal salePrice, boolean priceOverridden, int stock) {

	// 後台以繁中顯示，沒有翻譯則用原文
	public static AdminCardView from(Card c, CardTranslation tr, BigDecimal rate) {
		String name = tr != null ? tr.getCardName() : c.getCardName();
		return new AdminCardView(c.getId(), c.getCardSetId(), name, c.getCardName(), c.getRarity(),
				c.getImageUrl(), c.getMarketPrice(), c.listPrice(rate), c.getExtraDiscount(),
				c.getSalePrice(), c.isPriceOverridden(), c.getStock());
	}
}
