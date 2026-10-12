package com.argo.common.mail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

// 有設 spring.mail.host 才真的寄，否則只寫日誌
@Component
class DefaultMailer implements Mailer {

	private static final Logger log = LoggerFactory.getLogger(DefaultMailer.class);

	private final ObjectProvider<JavaMailSender> sender;
	private final String from;

	DefaultMailer(ObjectProvider<JavaMailSender> sender, @Value("${argo.mail.from}") String from) {
		this.sender = sender;
		this.from = from;
	}

	@Override
	public void send(String to, String subject, String body) {
		JavaMailSender real = sender.getIfAvailable();
		if (real == null) {
			log.info("Mail (not sent) to={} subject={}\n{}", to, subject, body);
			return;
		}
		SimpleMailMessage msg = new SimpleMailMessage();
		msg.setFrom(from);
		msg.setTo(to);
		msg.setSubject(subject);
		msg.setText(body);
		try {
			real.send(msg);
		}
		catch (RuntimeException e) {
			// 寄信失敗不影響主流程
			log.warn("Mail send failed to={}: {}", to, e.getMessage());
		}
	}
}
