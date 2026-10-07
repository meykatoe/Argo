package com.argo.card.i18n;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

@Entity
@Table(name = "card_translation")
public class CardTranslation {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private String cardSetId;

	@Column(nullable = false)
	private String locale;

	@Column(nullable = false)
	private String cardName;

	private String cardText;

	private String subTypes;

	@Column(nullable = false)
	private OffsetDateTime updatedAt = OffsetDateTime.now();

	protected CardTranslation() {
	}

	public CardTranslation(String cardSetId, String locale) {
		this.cardSetId = cardSetId;
		this.locale = locale;
	}

	public String getCardSetId() {
		return cardSetId;
	}

	public String getLocale() {
		return locale;
	}

	public String getCardName() {
		return cardName;
	}

	public String getCardText() {
		return cardText;
	}

	public String getSubTypes() {
		return subTypes;
	}

	public void fill(String cardName, String cardText, String subTypes) {
		this.cardName = cardName;
		this.cardText = cardText;
		this.subTypes = subTypes;
		this.updatedAt = OffsetDateTime.now();
	}
}
