package com.argo.order;

import com.argo.card.CardStockService;
import com.argo.common.ApiException;
import com.argo.common.ErrorCode;
import com.argo.common.PageResult;
import com.argo.card.i18n.Locales;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {

	private static final Logger log = LoggerFactory.getLogger(OrderService.class);
	private static final int MAX_LINE_QTY = 99;
	private static final int MAX_PAGE_SIZE = 50;

	private final ShopOrderRepository orders;
	private final PaymentRepository payments;
	private final CardStockService stock;
	private final OrderNumberGenerator numbers;
	private final String currency;
	private final BigDecimal shippingFee;
	private final long expireMinutes;
	private final ApplicationEventPublisher events;
	private final ExchangeRateService fx;

	public OrderService(ShopOrderRepository orders, PaymentRepository payments, CardStockService stock,
			OrderNumberGenerator numbers,
			@Value("${argo.order.currency}") String currency,
			@Value("${argo.order.shipping-fee}") BigDecimal shippingFee,
			@Value("${argo.order.expire-minutes}") long expireMinutes,
			ApplicationEventPublisher events, ExchangeRateService fx) {
		this.orders = orders;
		this.payments = payments;
		this.stock = stock;
		this.numbers = numbers;
		this.currency = currency;
		this.shippingFee = shippingFee.setScale(2, RoundingMode.HALF_UP);
		this.expireMinutes = expireMinutes;
		this.events = events;
		this.fx = fx;
	}

	@Transactional
	public OrderView create(CreateOrderRequest req, String lang, Long customerId) {
		String locale = Locales.normalize(lang);
		// 同一張卡重複的列合併
		Map<Long, Integer> wanted = new TreeMap<>();
		for (CreateOrderRequest.Item item : req.items()) {
			int qty = wanted.merge(item.cardId(), item.quantity(), Integer::sum);
			if (qty > MAX_LINE_QTY) {
				throw new ApiException(ErrorCode.INVALID_QUANTITY,
						Map.of("cardId", String.valueOf(item.cardId())));
			}
		}

		ShopOrder order = new ShopOrder(newOrderNo(), currency, locale);
		order.assignCustomer(customerId);
		BigDecimal subtotal = BigDecimal.ZERO;
		for (CardStockService.Reserved r : stock.reserve(wanted, locale)) {
			OrderItem item = new OrderItem(r.cardId(), r.cardSetId(), r.name(), r.originalName(),
					r.imageUrl(), r.unitPrice(), r.quantity());
			order.addItem(item);
			subtotal = subtotal.add(item.getSubtotal());
		}

		order.setAmounts(subtotal, shippingFee);
		// 綠界只收新台幣，匯率在下單時鎖定
		var quote = fx.toTwd(order.getTotal());
		order.setTwd(quote.rate(), quote.twd());
		var c = req.customer();
		order.setCustomer(c.name().trim(), c.email().trim().toLowerCase(), c.phone().trim());
		var s = req.shipping();
		order.setShipping(s.recipientName().trim(), s.recipientPhone().trim(), s.postalCode().trim(),
				s.city().trim(), s.address().trim());
		orders.save(order);
		events.publishEvent(OrderNotice.of(OrderNotice.Kind.CREATED, order));
		return view(order);
	}

	// 顧客自己的訂單，新的在前
	@Transactional(readOnly = true)
	public PageResult<OrderView> listMine(Long customerId, int page, int size) {
		if (page < 1 || size < 1 || size > MAX_PAGE_SIZE) {
			throw new ApiException(ErrorCode.INVALID_PAGING);
		}
		var result = orders.findByCustomerId(customerId, PageRequest.of(page - 1, size,
				Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by(Sort.Direction.DESC, "id"))));
		return PageResult.of(result, this::view);
	}

	// 顧客 Email 已驗證，可把同 Email 的訪客訂單認領到帳號
	@Transactional(readOnly = true)
	public long claimableCount(String email) {
		return orders.countByCustomerIdIsNullAndCustomerEmail(email.trim().toLowerCase());
	}

	@Transactional
	public int claimGuestOrders(Long customerId, String email) {
		return orders.claimGuestOrders(customerId, email.trim().toLowerCase());
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
			throw new ApiException(ErrorCode.ORDER_NOT_CANCELLABLE);
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

	public void cancelInternal(ShopOrder order, String reason) {
		order.markCancelled(reason);
		order.getItems().stream()
				.sorted(Comparator.comparing(OrderItem::getCardId))
				.forEach(i -> stock.release(i.getCardId(), i.getQuantity()));
		events.publishEvent(OrderNotice.of(OrderNotice.Kind.CANCELLED, order));
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

	private static ApiException notFound() {
		// 不透露是編號還是信箱不符
		return new ApiException(ErrorCode.ORDER_NOT_FOUND);
	}
}
