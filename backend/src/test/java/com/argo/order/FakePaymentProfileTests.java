package com.argo.order;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class FakePaymentProfileTests {

	private final ApplicationContextRunner runner = new ApplicationContextRunner()
			.withUserConfiguration(FakePaymentController.class)
			.withBean(PaymentService.class, () -> org.mockito.Mockito.mock(PaymentService.class));

	@Test
	void absentInProd() {
		runner.withInitializer(c -> c.getEnvironment().setActiveProfiles("prod"))
				.run(ctx -> assertThat(ctx).doesNotHaveBean(FakePaymentController.class));
	}

	@Test
	void presentByDefault() {
		runner.run(ctx -> assertThat(ctx).hasSingleBean(FakePaymentController.class));
	}
}
