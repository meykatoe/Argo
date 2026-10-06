package com.argo.admin;

import java.math.BigDecimal;

// 折扣範圍相同代表整個系列折扣一致
public record AdminCardSetView(String setId, String setName, String setNameEn, String category,
		int onSale, long cardCount, BigDecimal minDiscount, BigDecimal maxDiscount) {
}
