package com.argo.card.i18n;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.util.Objects;

@Entity
@Table(name = "card_set_translation")
@IdClass(CardSetTranslation.Key.class)
public class CardSetTranslation {

	@Id
	private String setId;

	@Id
	private String locale;

	@Column(nullable = false)
	private String setName;

	protected CardSetTranslation() {
	}

	public CardSetTranslation(String setId, String locale, String setName) {
		this.setId = setId;
		this.locale = locale;
		this.setName = setName;
	}

	public String getSetId() {
		return setId;
	}

	public String getLocale() {
		return locale;
	}

	public String getSetName() {
		return setName;
	}

	public void setSetName(String setName) {
		this.setName = setName;
	}

	public static class Key implements Serializable {

		private String setId;
		private String locale;

		public Key() {
		}

		@Override
		public boolean equals(Object o) {
			return o instanceof Key k && Objects.equals(setId, k.setId) && Objects.equals(locale, k.locale);
		}

		@Override
		public int hashCode() {
			return Objects.hash(setId, locale);
		}
	}
}
