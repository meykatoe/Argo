package com.argo.card;

import com.argo.common.Flag;

public record CardSetDto(String setId, String setName, String setNameEn, String category,
		int onSale) {

	public static CardSetDto from(CardSet s, String localName) {
		return new CardSetDto(s.getSetId(), localName != null ? localName : s.getSetName(),
				s.getSetName(), s.getCategory(), Flag.of(s.isOnSale()));
	}
}
