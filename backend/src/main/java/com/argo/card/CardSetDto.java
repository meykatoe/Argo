package com.argo.card;

public record CardSetDto(String setId, String setName, String category) {

	public static CardSetDto from(CardSet s) {
		return new CardSetDto(s.getSetId(), s.getSetName(), s.getCategory());
	}
}
