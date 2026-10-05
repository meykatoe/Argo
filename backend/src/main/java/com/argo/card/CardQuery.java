package com.argo.card;

public record CardQuery(String keyword, String setId, String category, String color,
		String rarity, String cardType) {
}
