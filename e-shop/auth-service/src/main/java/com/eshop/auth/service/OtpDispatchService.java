package com.eshop.auth.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class OtpDispatchService {
    private static final Logger log = LoggerFactory.getLogger(OtpDispatchService.class);

    private final ObjectProvider<RabbitTemplate> rabbitTemplateProvider;
    private final EmailSenderService emailSenderService;
    private final SmsSenderService smsSenderService;

    @Value("${app.otp.rabbit.enabled:false}")
    private boolean rabbitEnabled;

    @Value("${app.otp.rabbit.exchange:auth.otp.exchange}")
    private String otpExchange;

    @Value("${app.otp.rabbit.routing-key:auth.otp}")
    private String otpRoutingKey;

    public OtpDispatchService(
            ObjectProvider<RabbitTemplate> rabbitTemplateProvider,
            EmailSenderService emailSenderService,
            SmsSenderService smsSenderService
    ) {
        this.rabbitTemplateProvider = rabbitTemplateProvider;
        this.emailSenderService = emailSenderService;
        this.smsSenderService = smsSenderService;
    }

    public void sendOtp(OtpDeliveryChannel channel, String email, String phoneNumber, String subject, String messageBody) {
        OtpDispatchMessage payload = new OtpDispatchMessage(channel, email, phoneNumber, subject, messageBody);
        if (rabbitEnabled) {
            RabbitTemplate rabbitTemplate = rabbitTemplateProvider.getIfAvailable();
            if (rabbitTemplate != null) {
                try {
                    rabbitTemplate.convertAndSend(otpExchange, otpRoutingKey, payload);
                    log.info("OTP dispatched to RabbitMQ via {}.", channel);
                    return;
                } catch (Exception ex) {
                    log.warn("RabbitMQ OTP dispatch failed: {}. Falling back to direct send.", ex.getMessage());
                }
            } else {
                log.warn("RabbitMQ OTP is enabled but RabbitTemplate is unavailable. Falling back to direct send.");
            }
        }

        directSend(payload);
    }

    public void directSend(OtpDispatchMessage message) {
        if (message == null) {
            return;
        }
        OtpDeliveryChannel channel = message.channel() == null ? OtpDeliveryChannel.EMAIL : message.channel();
        if (channel == OtpDeliveryChannel.SMS) {
            boolean sentSms = smsSenderService.sendOtpSms(message.phoneNumber(), message.messageBody());
            if (!sentSms && message.email() != null && !message.email().isBlank()) {
                log.warn("SMS OTP send failed. Falling back to email for {}", message.email());
                emailSenderService.sendOtpEmail(message.email(), message.subject(), message.messageBody());
            }
            return;
        }
        emailSenderService.sendOtpEmail(message.email(), message.subject(), message.messageBody());
    }
}
