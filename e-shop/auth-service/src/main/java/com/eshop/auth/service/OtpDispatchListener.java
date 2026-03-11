package com.eshop.auth.service;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "app.otp.rabbit.enabled", havingValue = "true")
public class OtpDispatchListener {

    private final OtpDispatchService otpDispatchService;

    public OtpDispatchListener(OtpDispatchService otpDispatchService) {
        this.otpDispatchService = otpDispatchService;
    }

    @RabbitListener(queues = "${app.otp.rabbit.queue:auth.otp.queue}")
    public void consumeOtpDispatch(OtpDispatchMessage message) {
        otpDispatchService.directSend(message);
    }
}
