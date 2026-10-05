package com.argo.order;

import com.argo.card.Card;
import com.argo.card.CardRepository;
import com.argo.common.ApiException;
import com.argo.i18n.CardTranslation;
import com.argo.i18n.CardTranslationRepository;
import com.argo.i18n.Locales;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {

	private static final Logger log = LoggerFactory.getLogger(OrderService.class);
	private static final int MAX_LINE_QTY = 99;

	private final ShopOrderRepository orders;
	private final PaymentRepository payments;
	private final CardRepository cards;
	private final CardTranslationRepository translations;
	private final OrderNumberGenerator numbers;
	private final String currency;
	private final BigDecimal shippingFee;
	private final long expireMinutes;

	public OrderService(ShopOrderRepository orders, PaymentRepository payments, CardRepository cards,
			CardTranslationRepository translations, OrderNumberGenerator numbers,
			@Value("${argo.order.currency}") String currency,
			@Value("${argo.order.shipping-fee}") BigDecimal shippingFee,
			@Value("${argo.order.expire-minutes}") long expireMinutes) {
		this.orders = orders;
		this.payments = payments;
		this.cards = cards;
		this.translations = translations;
		this.numbers = numbers;
		this.currency = currency;
		this.shippingFee = shippingFee.setScale(2, RoundingMode.HALF_UP);
		this.expireMinutes = expireMinutes;
	}

	@Transactional
	public OrderView create(CreateOrderRequest req, String lang) {
		String locale = Locales.normalize(lang);
		// 同一張卡重複的列合併
		Map<Long, Integer> wanted = new TreeMap<>();
		for (CreateOrderRequest.Item item : req.items()) {
			int qty = wanted.merge(item.cardId(), item.quantity(), Integer::sum);
			if (qty > MAX_LINE_QTY) {
				throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_QUANTITY",
						Map.of("cardId", String.valueOf(item.cardId())));
			}
		}

		Map<Long, Card> found = cards.findAllById(wanted.keySet()).stream()
				.collect(Collectors.toMap(Card::getId, Function.identity()));
		Map<String, CardTranslation> trs = translationsFor(locale, found.values());

		ShopOrder order = new ShopOrder(newOrderNo(), currency, locale);
		BigDecimal subtotal = BigDecimal.ZERO;
		// 依編號排序處理，避免死鎖
		for (Map.Entry<Long, Integer> e : wanted.entrySet()) {
			Card card = found.get(e.getKey());
			Map<String, String> detail = Map.of("cardId", String.valueOf(e.getKey()));
			if (card == null) {
				throw new ApiException(HttpStatus.BAD_REQUEST, "ITEM_NOT_FOUND", detail);
			}
			if (card.getStock() <= 0 || card.getSalePrice().signum() <= 0) {
				throw new ApiException(HttpStatus.CONFLICT, "ITEM_UNAVAILABLE", detail);
			}
			// 單一語句扣庫存，擋住同時下單
			if (cards.decrementStock(card.getId(), e.getValue()) == 0) {
				throw new ApiException(HttpStatus.CONFLICT, "INSUFFICIENT_STOCK", detail);
			}
			CardTranslation tr = trs.get(card.getCardSetId());
			OrderItem item = new OrderItem(card.getId(), card.getCardSetId(),
					tr != null ? tr.getCardName() : card.getCardName(), card.getCardName(),
					card.getImageUrl(), card.getSalePrice(), e.getValue());
			order.addItem(item);
			subtotal = subtotal.add(item.getSubtotal());
		}

		order.setAmounts(subtotal, shippingFee);
		var c = req.customer();
		order.setCustomer(c.name().trim(), c.email().trim().toLowerCase(), c.phone().trim());
		var s = req.shipping();
		order.setShipping(s.recipientName().trim(), s.recipientPhone().trim(), s.postalCode().trim(),
				s.city().trim(), s.address().trim());
		orders.save(order);
		return view(order);
	}

	@Transactional(readOnly = true)
	public OrderView get(String orderNo, String email) {
		return view(orders.findByOrderNo(orderNo)
				.filter(o -> o.emailMatches(email))
				.orElseThrow(OrderService::notFound));
	}

	@Transactional
	public OrderView cancel(String orderNo, String email) {
		ShopOrder order = findForUpdate(orderNo, email);
		if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
			throw new ApiException(HttpStatus.CONFLICT, "ORDER_NOT_CANCELLABLE");
		}
		cancelInternal(order, "CUSTOMER");
		return view(order);
	}

	// 取消逾時未付款的訂單並歸還庫存
	@Transactional
	public int expireOverdue() {
		OffsetDateTime limit = OffsetDateTime.now().minusMinutes(expireMinutes);
		int count = 0;
		for (ShopOrder o : orders.findByStatusAndCreatedAtBefore(OrderStatus.PENDING_PAYMENT, limit)) {
			// 重新鎖定並確認狀態，避免剛好付款
			ShopOrder locked = orders.findByOrderNoForUpdate(o.getOrderNo()).orElse(null);
			if (locked != null && locked.getStatus() == OrderStatus.PENDING_PAYMENT) {
				cancelInternal(locked, "EXPIRED");
				count++;
			}
		}
		if (count > 0) {
			log.info("已取消逾時訂單 {} 筆", count);
		}
		return count;
	}

	ShopOrder findForUpdate(String orderNo, String email) {
		return orders.findByOrderNoForUpdate(orderNo)
				.filter(o -> o.emailMatches(email))
				.orElseThrow(OrderService::notFound);
	}

	boolean isExpired(ShopOrder order) {
		return order.getCreatedAt().plusMinutes(expireMinutes).isBefore(OffsetDateTime.now());
	}

	void cancelInternal(ShopOrder order, String reason) {
		order.markCancelled(reason);
		order.getItems().stream()
				.sorted(Comparator.comparing(OrderItem::getCardId))
				.forEach(i -> cards.incrementStock(i.getCardId(), i.getQuantity()));
	}

	OrderView view(ShopOrder order) {
		Payment last = payments.findFirstByOrderIdOrderByIdDesc(order.getId()).orElse(null);
		return OrderView.from(order, last, expireMinutes);
	}

	private String newOrderNo() {
		for (int i = 0; i < 5; i++) {
			String no = numbers.next();
			if (!orders.existsByOrderNo(no)) {
				return no;
			}
		}
		throw new IllegalStateException("無法產生訂單編號");
	}

	private Map<String, CardTranslation> translationsFor(String locale, java.util.Collection<Card> list) {
		if (Locales.isDefault(locale) || list.isEmpty()) {
			return Map.of();
		}
		List<String> ids = list.stream().map(Card::getCardSetId).distinct().toList();
		return translations.findByLocaleAndCardSetIdIn(locale, ids).stream()
				.collect(Collectors.toMap(CardTranslation::getCardSetId, Function.identity()));
	}

	private static ApiException notFound() {
		// 不透露是編號還是信箱不符
		return new ApiException(HttpStatus.NOT_FOUND, "ORDER_NOT_FOUND");
	}
}
