package com.argo.order;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "shop_order")
public class ShopOrder {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true)
	private String orderNo;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private OrderStatus status = OrderStatus.PENDING_PAYMENT;

	@Column(nullable = false)
	private String currency;

	@Column(nullable = false)
	private BigDecimal subtotal;

	@Column(nullable = false)
	private BigDecimal shippingFee;

	@Column(nullable = false)
	private BigDecimal total;

	@Column(nullable = false)
	private String customerName;

	@Column(nullable = false)
	private String customerEmail;

	@Column(nullable = false)
	private String customerPhone;

	@Column(nullable = false)
	private String recipientName;

	@Column(nullable = false)
	private String recipientPhone;

	@Column(nullable = false)
	private String postalCode;

	@Column(nullable = false)
	private String city;

	@Column(nullable = false)
	private String address;

	@Column(nullable = false)
	private String lang;

	private String cancelReason;

	@Column(nullable = false)
	private OffsetDateTime createdAt = OffsetDateTime.now();

	@Column(nullable = false)
	private OffsetDateTime updatedAt = OffsetDateTime.now();

	private OffsetDateTime paidAt;

	private OffsetDateTime cancelledAt;

	@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
	@JoinColumn(name = "order_id", nullable = false)
	private List<OrderItem> items = new ArrayList<>();

	protected ShopOrder() {
	}

	public ShopOrder(String orderNo, String currency, String lang) {
		this.orderNo = orderNo;
		this.currency = currency;
		this.lang = lang;
	}

	public void setAmounts(BigDecimal subtotal, BigDecimal shippingFee) {
		this.subtotal = subtotal;
		this.shippingFee = shippingFee;
		this.total = subtotal.add(shippingFee);
	}

	public void setCustomer(String name, String email, String phone) {
		this.customerName = name;
		this.customerEmail = email;
		this.customerPhone = phone;
	}

	public void setShipping(String recipientName, String recipientPhone, String postalCode,
			String city, String address) {
		this.recipientName = recipientName;
		this.recipientPhone = recipientPhone;
		this.postalCode = postalCode;
		this.city = city;
		this.address = address;
	}

	public void addItem(OrderItem item) {
		items.add(item);
	}

	public void markPaid() {
		this.status = OrderStatus.PAID;
		this.paidAt = OffsetDateTime.now();
		this.updatedAt = this.paidAt;
	}

	public void markCancelled(String reason) {
		this.status = OrderStatus.CANCELLED;
		this.cancelReason = reason;
		this.cancelledAt = OffsetDateTime.now();
		this.updatedAt = this.cancelledAt;
	}

	public boolean emailMatches(String email) {
		return email != null && customerEmail.equalsIgnoreCase(email.trim());
	}

	public Long getId() {
		return id;
	}

	public String getOrderNo() {
		return orderNo;
	}

	public OrderStatus getStatus() {
		return status;
	}

	public String getCurrency() {
		return currency;
	}

	public BigDecimal getSubtotal() {
		return subtotal;
	}

	public BigDecimal getShippingFee() {
		return shippingFee;
	}

	public BigDecimal getTotal() {
		return total;
	}

	public String getCustomerName() {
		return customerName;
	}

	public String getCustomerEmail() {
		return customerEmail;
	}

	public String getCustomerPhone() {
		return customerPhone;
	}

	public String getRecipientName() {
		return recipientName;
	}

	public String getRecipientPhone() {
		return recipientPhone;
	}

	public String getPostalCode() {
		return postalCode;
	}

	public String getCity() {
		return city;
	}

	public String getAddress() {
		return address;
	}

	public String getLang() {
		return lang;
	}

	public String getCancelReason() {
		return cancelReason;
	}

	public OffsetDateTime getCreatedAt() {
		return createdAt;
	}

	public OffsetDateTime getPaidAt() {
		return paidAt;
	}

	public OffsetDateTime getCancelledAt() {
		return cancelledAt;
	}

	public List<OrderItem> getItems() {
		return items;
	}
}
