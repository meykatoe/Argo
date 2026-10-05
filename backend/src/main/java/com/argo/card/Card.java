package com.argo.card;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(name = "card")
public class Card {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true)
	private String sourceKey;

	@Column(nullable = false)
	private String cardSetId;

	@Column(nullable = false)
	private String cardImageId;

	@Column(nullable = false)
	private String setId;

	@Column(nullable = false)
	private String cardName;

		private String cardText;

	@Column(nullable = false)
	private String rarity;

	@Column(nullable = false)
	private String cardColor;

	@Column(nullable = false)
	private String cardType;

		private String cardCost;

		private String cardPower;

		private Integer counterAmount;

	private String life;

	private String attribute;

		private String subTypes;

		private String imageUrl;

	@Column(nullable = false)
	private BigDecimal marketPrice = BigDecimal.ZERO;

	@Column(nullable = false)
	private BigDecimal inventoryPrice = BigDecimal.ZERO;

		private LocalDate dateScraped;

	@Column(insertable = false, updatable = false)
	private OffsetDateTime createdAt;

	@Column(nullable = false)
	private OffsetDateTime updatedAt = OffsetDateTime.now();

	protected Card() {
	}

	public Card(String sourceKey) {
		this.sourceKey = sourceKey;
	}

	public Long getId() {
		return id;
	}

	public String getSourceKey() {
		return sourceKey;
	}

	public String getCardSetId() {
		return cardSetId;
	}

	public String getCardImageId() {
		return cardImageId;
	}

	public String getSetId() {
		return setId;
	}

	public String getCardName() {
		return cardName;
	}

	public String getCardText() {
		return cardText;
	}

	public String getRarity() {
		return rarity;
	}

	public String getCardColor() {
		return cardColor;
	}

	public String getCardType() {
		return cardType;
	}

	public String getCardCost() {
		return cardCost;
	}

	public String getCardPower() {
		return cardPower;
	}

	public Integer getCounterAmount() {
		return counterAmount;
	}

	public String getLife() {
		return life;
	}

	public String getAttribute() {
		return attribute;
	}

	public String getSubTypes() {
		return subTypes;
	}

	public String getImageUrl() {
		return imageUrl;
	}

	public BigDecimal getMarketPrice() {
		return marketPrice;
	}

	public BigDecimal getInventoryPrice() {
		return inventoryPrice;
	}

	public LocalDate getDateScraped() {
		return dateScraped;
	}

	public void fill(OptcgCard src) {
		this.cardSetId = src.cardSetId();
		this.cardImageId = src.cardImageId();
		this.setId = src.setId();
		this.cardName = src.cardName();
		this.cardText = src.cardText();
		this.rarity = src.rarity();
		this.cardColor = src.cardColor();
		this.cardType = src.cardType();
		this.cardCost = src.cardCost();
		this.cardPower = src.cardPower();
		this.counterAmount = src.counterAmount();
		this.life = src.life();
		this.attribute = src.attribute();
		this.subTypes = src.subTypes();
		this.imageUrl = src.cardImage();
		this.marketPrice = BigDecimal.valueOf(src.marketPrice());
		this.inventoryPrice = BigDecimal.valueOf(src.inventoryPrice());
		this.dateScraped = src.scrapedDate();
		this.updatedAt = OffsetDateTime.now();
	}
}
