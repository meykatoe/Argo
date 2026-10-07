package com.argo.card.i18n;

public final class Locales {

	public static final String EN = "en";
	public static final String ZH_TW = "zh-TW";

	private Locales() {
	}

	// 不支援的語系用英文
	public static String normalize(String lang) {
		return ZH_TW.equalsIgnoreCase(lang) ? ZH_TW : EN;
	}

	public static boolean isDefault(String locale) {
		return EN.equals(locale);
	}
}
