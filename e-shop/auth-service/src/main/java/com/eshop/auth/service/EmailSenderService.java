package com.eshop.auth.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailSenderService {
    private static final Logger log = LoggerFactory.getLogger(EmailSenderService.class);

    private final ObjectProvider<JavaMailSender> mailSenderProvider;

    @Value("${app.email.from:no-reply@eshop.local}")
    private String from;

    public EmailSenderService(ObjectProvider<JavaMailSender> mailSenderProvider) {
        this.mailSenderProvider = mailSenderProvider;
    }

    public void sendOtpEmail(String to, String subject, String messageBody) {
        JavaMailSender sender = mailSenderProvider.getIfAvailable();
        if (sender == null) {
            log.info("Mail sender not configured. OTP for {} [{}]: {}", to, subject, messageBody);
            return;
        }
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setFrom(from);
            msg.setTo(to);
            msg.setSubject(subject);
            msg.setText(messageBody);
            sender.send(msg);
        } catch (Exception e) {
            log.warn("Failed to send email to {}: {} — OTP for dev: [{}] {}", to, e.getMessage(), subject, messageBody);
        }
    }
}
