package com.argo.common.mail;

public interface Mailer {

	void send(String to, String subject, String body);
}
