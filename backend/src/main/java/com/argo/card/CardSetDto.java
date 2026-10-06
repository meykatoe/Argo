package com.argo.card;

public record CardSetDto(String setId, String setName, String setNameEn, String category,
		boolean onSale) {

	public static CardSetDto from(CardSet s, String localName) {
		return new CardSetDto(s.getSetId(), localName != null ? localName : s.getSetName(),
				s.getSetName(), s.getCategory(), s.isOnSale());
	}
}
