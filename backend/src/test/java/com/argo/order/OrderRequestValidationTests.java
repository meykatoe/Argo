package com.argo.order;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.List;
import org.junit.jupiter.api.Test;

class OrderRequestValidationTests {

	private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

	private CreateOrderRequest.Customer customer(String email, String phone) {
		return new CreateOrderRequest.Customer("王小明", email, phone);
	}

	private CreateOrderRequest.Shipping shipping(String phone, String postal) {
		return new CreateOrderRequest.Shipping("王小明", phone, postal, "台北市", "中正區");
	}

	private CreateOrderRequest req(List<CreateOrderRequest.Item> items, CreateOrderRequest.Customer c,
			CreateOrderRequest.Shipping s) {
		return new CreateOrderRequest(items, c, s);
	}

	private List<CreateOrderRequest.Item> one() {
		return List.of(new CreateOrderRequest.Item(1L, 1));
	}

	@Test
	void validRequestPasses() {
		assertTrue(validator.validate(req(one(), customer("a@b.co", "0912345678"),
				shipping("02-2345-6789", "100"))).isEmpty());
		assertTrue(validator.validate(req(one(), customer("a@b.co", "+886912345678"),
				shipping("0912345678", "10058"))).isEmpty());
	}

	@Test
	void rejectsBadFields() {
		assertEquals(1, validator.validate(req(List.of(), customer("a@b.co", "0912345678"),
				shipping("0912345678", "100"))).size());
		assertEquals(1, validator.validate(req(one(), customer("not-mail", "0912345678"),
				shipping("0912345678", "100"))).size());
		assertEquals(1, validator.validate(req(one(), customer("a@b.co", "abc"),
				shipping("0912345678", "100"))).size());
		assertEquals(1, validator.validate(req(one(), customer("a@b.co", "0912345678"),
				shipping("0912345678", "12"))).size());
		assertEquals(1, validator.validate(req(List.of(new CreateOrderRequest.Item(1L, 0)),
				customer("a@b.co", "0912345678"), shipping("0912345678", "100"))).size());
		assertEquals(1, validator.validate(req(List.of(new CreateOrderRequest.Item(1L, 100)),
				customer("a@b.co", "0912345678"), shipping("0912345678", "100"))).size());
	}
}
