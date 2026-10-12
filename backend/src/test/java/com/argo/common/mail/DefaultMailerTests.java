package com.argo.common.mail;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

class DefaultMailerTests {

	@SuppressWarnings("unchecked")
	private static ObjectProvider<JavaMailSender> provider(JavaMailSender s) {
		ObjectProvider<JavaMailSender> p = mock(ObjectProvider.class);
		when(p.getIfAvailable()).thenReturn(s);
		return p;
	}

	@Test
	void withoutSmtpOnlyLogs() {
		DefaultMailer m = new DefaultMailer(provider(null), "no-reply@argo.test");
		assertDoesNotThrow(() -> m.send("a@example.com", "s", "b"));
	}

	@Test
	void sendsThroughSmtp() {
		JavaMailSender real = mock(JavaMailSender.class);
		new DefaultMailer(provider(real), "no-reply@argo.test").send("a@example.com", "s", "b");
		verify(real).send(any(SimpleMailMessage.class));
	}

	@Test
	void smtpFailureIsSwallowed() {
		JavaMailSender real = mock(JavaMailSender.class);
		doThrow(new RuntimeException("down")).when(real).send(any(SimpleMailMessage.class));
		DefaultMailer m = new DefaultMailer(provider(real), "no-reply@argo.test");
		assertDoesNotThrow(() -> m.send("a@example.com", "s", "b"));
	}
}
