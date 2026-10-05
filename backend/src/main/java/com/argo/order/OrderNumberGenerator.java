package com.argo.order;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import org.springframework.stereotype.Component;

@Component
public class OrderNumberGenerator {

	// 去掉容易看錯的字元
	private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
	private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyMMdd");

	private final SecureRandom random = new SecureRandom();

	// 例如 AR261005-K7P3QX
	public String next() {
		StringBuilder sb = new StringBuilder("AR").append(LocalDate.now().format(DATE)).append('-');
		for (int i = 0; i < 6; i++) {
			sb.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
		}
		return sb.toString();
	}
}
