package com.argo.order;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

public record CreateOrderRequest(
		@NotEmpty @Size(max = 50) List<@Valid @NotNull Item> items,
		@NotNull @Valid Customer customer,
		@NotNull @Valid Shipping shipping) {

	private static final String PHONE = "^[0-9+\\-() ]{8,20}$";

	public record Item(@NotNull Long cardId, @Min(1) @Max(99) int quantity) {
	}

	public record Customer(
			@NotBlank @Size(max = 100) String name,
			@NotBlank @Email @Size(max = 200) String email,
			@NotBlank @Pattern(regexp = PHONE) String phone) {
	}

	public record Shipping(
			@NotBlank @Size(max = 100) String recipientName,
			@NotBlank @Pattern(regexp = PHONE) String recipientPhone,
			@NotBlank @Pattern(regexp = "^[0-9]{3}([0-9]{2,3})?$") String postalCode,
			@NotBlank @Size(max = 50) String city,
			@NotBlank @Size(max = 200) String address) {
	}
}
