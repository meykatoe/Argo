package com.argo.card;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDate;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OptcgCard(
		@JsonProperty("card_set_id") String cardSetId,
		@JsonProperty("card_image_id") String cardImageId,
		@JsonProperty("set_id") String setId,
		@JsonProperty("set_name") String setName,
		@JsonProperty("card_name") String cardName,
		@JsonProperty("card_text") String cardText,
		@JsonProperty("rarity") String rarity,
		@JsonProperty("card_color") String cardColor,
		@JsonProperty("card_type") String cardType,
		@JsonProperty("card_cost") String cardCost,
		@JsonProperty("card_power") String cardPower,
		@JsonProperty("counter_amount") Integer counterAmount,
		@JsonProperty("life") String life,
		@JsonProperty("attribute") String attribute,
		@JsonProperty("sub_types") String subTypes,
		@JsonProperty("card_image") String cardImage,
		@JsonProperty("market_price") double marketPrice,
		@JsonProperty("inventory_price") double inventoryPrice,
		@JsonProperty("date_scraped") String dateScraped) {

	// 區分同卡不同版
	public String sourceKey() {
		String file = "";
		if (cardImage != null) {
			file = cardImage.substring(cardImage.lastIndexOf('/') + 1);
		}
		return setId + "|" + cardImageId + "|" + file;
	}

	// 格式不符回空值
	public LocalDate scrapedDate() {
		try {
			return LocalDate.parse(dateScraped);
		} catch (RuntimeException e) {
			return null;
		}
	}
}
