package com.argo.card;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "card_set")
public class CardSet {

	@Id
		private String setId;

	@Column(nullable = false)
	private String setName;

	@Column(nullable = false)
	private String category;

	// 下架後仍顯示，但不可購買
	@Column(nullable = false)
	private boolean onSale = true;

	protected CardSet() {
	}

	public CardSet(String setId, String setName, String category) {
		this.setId = setId;
		this.setName = setName;
		this.category = category;
	}

	public String getSetId() {
		return setId;
	}

	public String getSetName() {
		return setName;
	}

	public String getCategory() {
		return category;
	}

	public boolean isOnSale() {
		return onSale;
	}

	public void setOnSale(boolean onSale) {
		this.onSale = onSale;
	}

	public void setSetName(String setName) {
		this.setName = setName;
	}
}
