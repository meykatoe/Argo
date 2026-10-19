package com.argo.order;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

// 綠界 AioCheckOut 參數與 CheckMacValue
@Component
public class EcpayCheckout {

	static final String MAC = "CheckMacValue";
	private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
	private static final ZoneId TAIPEI = ZoneId.of("Asia/Taipei");

	// 前端要送出的表單
	public record Form(String action, Map<String, String> fields) {
	}

	private final String merchantId;
	private final String hashKey;
	private final String hashIv;
	private final String checkoutUrl;

	public EcpayCheckout(@Value("${argo.ecpay.merchant-id}") String merchantId,
			@Value("${argo.ecpay.hash-key}") String hashKey, @Value("${argo.ecpay.hash-iv}") String hashIv,
			@Value("${argo.ecpay.checkout-url}") String checkoutUrl) {
		this.merchantId = merchantId;
		this.hashKey = hashKey;
		this.hashIv = hashIv;
		this.checkoutUrl = checkoutUrl;
	}

	public Form build(String tradeNo, long totalTwd, String itemName, String returnUrl,
			String orderResultUrl, String clientBackUrl) {
		return build(tradeNo, totalTwd, itemName, returnUrl, orderResultUrl, clientBackUrl,
				ZonedDateTime.now(TAIPEI));
	}

	Form build(String tradeNo, long totalTwd, String itemName, String returnUrl, String orderResultUrl,
			String clientBackUrl, ZonedDateTime now) {
		Map<String, String> f = new LinkedHashMap<>();
		f.put("MerchantID", merchantId);
		f.put("MerchantTradeNo", tradeNo);
		f.put("MerchantTradeDate", now.format(DATE));
		f.put("PaymentType", "aio");
		f.put("TotalAmount", Long.toString(totalTwd));
		f.put("TradeDesc", "Argo");
		f.put("ItemName", itemName);
		f.put("ReturnURL", returnUrl);
		f.put("OrderResultURL", orderResultUrl);
		f.put("ClientBackURL", clientBackUrl);
		f.put("ChoosePayment", "Credit");
		f.put("EncryptType", "1");
		f.put(MAC, mac(f));
		return new Form(checkoutUrl, f);
	}

	// 查詢交易的參數，時間戳為秒
	public Map<String, String> queryParams(String tradeNo, long timestamp) {
		Map<String, String> f = new LinkedHashMap<>();
		f.put("MerchantID", merchantId);
		f.put("MerchantTradeNo", tradeNo);
		f.put("TimeStamp", Long.toString(timestamp));
		f.put(MAC, mac(f));
		return f;
	}

	// 驗證綠界回傳的參數，用常數時間比對
	public boolean verify(Map<String, String> params) {
		String given = params.get(MAC);
		if (given == null) {
			return false;
		}
		return MessageDigest.isEqual(mac(params).getBytes(StandardCharsets.UTF_8),
				given.toUpperCase().getBytes(StandardCharsets.UTF_8));
	}

	String mac(Map<String, String> params) {
		TreeMap<String, String> sorted = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
		params.forEach((k, v) -> {
			if (!MAC.equals(k)) {
				sorted.put(k, v == null ? "" : v);
			}
		});
		StringBuilder raw = new StringBuilder("HashKey=").append(hashKey);
		sorted.forEach((k, v) -> raw.append('&').append(k).append('=').append(v));
		raw.append("&HashIV=").append(hashIv);
		String encoded = URLEncoder.encode(raw.toString(), StandardCharsets.UTF_8).toLowerCase()
				.replace("%2d", "-").replace("%5f", "_").replace("%2e", ".").replace("%21", "!")
				.replace("%2a", "*").replace("%28", "(").replace("%29", ")");
		try {
			byte[] digest = MessageDigest.getInstance("SHA-256").digest(encoded.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().withUpperCase().formatHex(digest);
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
	}
}
