package com.argo.order;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "shop_order_item")
public class OrderItem {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private Long cardId;

	@Column(nullable = false)
	private String cardSetId;

	@Column(nullable = false)
	private String cardName;

	@Column(nullable = false)
	private String cardNameEn;

	private String imageUrl;

	@Column(nullable = false)
	private BigDecimal unitPrice;

	@Column(nullable = false)
	private int quantity;

	@Column(nullable = false)
	private BigDecimal subtotal;

	protected OrderItem() {
	}

	public OrderItem(Long cardId, String cardSetId, String cardName, String cardNameEn,
			String imageUrl, BigDecimal unitPrice, int quantity) {
		this.cardId = cardId;
		this.cardSetId = cardSetId;
		this.cardName = cardName;
		this.cardNameEn = cardNameEn;
		this.imageUrl = imageUrl;
		this.unitPrice = unitPrice;
		this.quantity = quantity;
		this.subtotal = unitPrice.multiply(BigDecimal.valueOf(quantity));
	}

	public Long getCardId() {
		return cardId;
	}

	public String getCardSetId() {
		return cardSetId;
	}

	public String getCardName() {
		return cardName;
	}

	public String getCardNameEn() {
		return cardNameEn;
	}

	public String getImageUrl() {
		return imageUrl;
	}

	public BigDecimal getUnitPrice() {
		return unitPrice;
	}

	public int getQuantity() {
		return quantity;
	}

	public BigDecimal getSubtotal() {
		return subtotal;
	}
}
