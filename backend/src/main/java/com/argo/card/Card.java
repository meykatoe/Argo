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

	// 同步比對用唯一鍵
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

	// 實際售價，同步時才重算
	@Column(nullable = false)
	private BigDecimal salePrice = BigDecimal.ZERO;

	// 手動定價後同步不覆蓋
	@Column(nullable = false)
	private boolean priceOverridden;

	@Column(nullable = false)
	private int stock;

		private LocalDate dateScraped;

	// 由資料庫填入
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

	public BigDecimal getSalePrice() {
		return salePrice;
	}

	public boolean isPriceOverridden() {
		return priceOverridden;
	}

	public int getStock() {
		return stock;
	}

	// 手動定價
	public void overridePrice(BigDecimal price) {
		this.salePrice = price;
		this.priceOverridden = true;
	}

	public BigDecimal getInventoryPrice() {
		return inventoryPrice;
	}

	public LocalDate getDateScraped() {
		return dateScraped;
	}

	// 以來源資料覆蓋
	public void fill(OptcgCard src, BigDecimal saleRate) {
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
		if (!priceOverridden) {
			this.salePrice = Pricing.salePrice(this.marketPrice, saleRate);
		}
		this.dateScraped = src.scrapedDate();
		this.updatedAt = OffsetDateTime.now();
	}
}
