package com.eshop.auth.service;

public record OtpDispatchMessage(
        OtpDeliveryChannel channel,
        String email,
        String phoneNumber,
        String subject,
        String messageBody
) {
}
