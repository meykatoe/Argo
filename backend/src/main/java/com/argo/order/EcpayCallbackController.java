package com.argo.order;

import com.argo.common.RawResponse;
import java.net.URI;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// 綠界規定回應純文字 1|OK 與頁面轉址，不能用 Result
@RawResponse
@RestController
@RequestMapping("/api/payments/ecpay")
public class EcpayCallbackController {

	private final EcpayCheckout ecpay;
	private final PaymentService payments;
	private final String siteUrl;

	public EcpayCallbackController(EcpayCheckout ecpay, PaymentService payments,
			@Value("${argo.site-url}") String siteUrl) {
		this.ecpay = ecpay;
		this.payments = payments;
		this.siteUrl = siteUrl;
	}

	// 背景通知
	@PostMapping(value = "/notify", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
	public ResponseEntity<String> notify(@RequestParam Map<String, String> params) {
		PaymentService.Outcome out = handle(params);
		String body = out.accepted() ? "1|OK" : "0|REJECTED";
		return ResponseEntity.ok().contentType(MediaType.TEXT_PLAIN).body(body);
	}

	// 顧客付款完成後瀏覽器被導回，順便處理結果再轉到官網
	@PostMapping(value = "/return", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
	public ResponseEntity<Void> back(@RequestParam Map<String, String> params) {
		PaymentService.Outcome out = handle(params);
		String target = siteUrl + "/pay/result" + (out.orderNo() == null ? "" : "?order=" + out.orderNo());
		return ResponseEntity.status(HttpStatus.SEE_OTHER).header(HttpHeaders.LOCATION, URI.create(target).toString())
				.build();
	}

	private PaymentService.Outcome handle(Map<String, String> params) {
		if (!ecpay.verify(params)) {
			return new PaymentService.Outcome(false, null);
		}
		long amount;
		try {
			amount = Long.parseLong(params.getOrDefault("TradeAmt", ""));
		} catch (NumberFormatException e) {
			return new PaymentService.Outcome(false, null);
		}
		String rtn = params.getOrDefault("RtnCode", "");
		return payments.complete(params.getOrDefault("MerchantTradeNo", ""), "1".equals(rtn), amount,
				params.get("TradeNo"), params.get("card4no"), "ECPAY_" + rtn);
	}
}
