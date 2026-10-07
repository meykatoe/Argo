package com.argo.card.i18n;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

// 解析官方繁中卡表頁面
public final class TcPageParser {

	public record Series(String siteId, String setId, String name) {
	}

	public record Card(String cardNumber, String name, String feature, String text) {
	}

	private static final Pattern SET_ID = Pattern.compile("【([A-Z]+-\\d+)】");

	private TcPageParser() {
	}

	public static List<Series> parseSeries(String html) {
		Document doc = Jsoup.parse(html);
		List<Series> list = new ArrayList<>();
		for (Element opt : doc.select("select[name=series] option")) {
			String value = opt.attr("value");
			if (value.isBlank()) {
				continue;
			}
			// 標籤內含跳脫的標籤
			String label = opt.text().replaceAll("<[^>]*>", " ").replaceAll("\\s+", " ").trim();
			Matcher m = SET_ID.matcher(label);
			String setId = m.find() ? m.group(1) : null;
			String name = SET_ID.matcher(label).replaceAll("").trim();
			list.add(new Series(value, setId, name));
		}
		return list;
	}

	public static List<Card> parseCards(String html) {
		Document doc = Jsoup.parse(html);
		List<Card> list = new ArrayList<>();
		for (Element dl : doc.select("dl.modalCol")) {
			Element number = dl.selectFirst(".infoCol span");
			Element name = dl.selectFirst(".cardName");
			if (number == null || name == null || name.text().isBlank()) {
				continue;
			}
			list.add(new Card(number.text().trim(), name.text().trim(),
					field(dl, ".feature"), field(dl, ".text")));
		}
		return list;
	}

	private static String field(Element dl, String css) {
		Element el = dl.selectFirst(css);
		if (el == null) {
			return null;
		}
		el.select("h3").remove();
		// 先把換行標籤保留下來
		el.select("br").append("\\n");
		String text = el.text().replace("\\n", "\n").replaceAll(" *\n *", "\n").trim();
		return text.isEmpty() || text.equals("-") ? null : text;
	}
}
