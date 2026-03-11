package com.eshop.auth.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Base64;

@Service
public class SmsSenderService {
    private static final Logger log = LoggerFactory.getLogger(SmsSenderService.class);
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${app.sms.enabled:false}")
    private boolean smsEnabled;

    @Value("${app.sms.provider:log}")
    private String smsProvider;

    @Value("${app.sms.msg91.base-url:https://control.msg91.com/api/sendhttp.php}")
    private String msg91BaseUrl;

    @Value("${app.sms.msg91.auth-key:}")
    private String msg91AuthKey;

    @Value("${app.sms.msg91.sender-id:ESHOPP}")
    private String msg91SenderId;

    @Value("${app.sms.msg91.route:4}")
    private String msg91Route;

    @Value("${app.sms.twilio.base-url:https://api.twilio.com/2010-04-01}")
    private String twilioBaseUrl;

    @Value("${app.sms.twilio.account-sid:}")
    private String twilioAccountSid;

    @Value("${app.sms.twilio.auth-token:}")
    private String twilioAuthToken;

    @Value("${app.sms.twilio.from-number:}")
    private String twilioFromNumber;

    public boolean sendOtpSms(String phoneNumber, String messageBody) {
        if (phoneNumber == null || phoneNumber.isBlank()) {
            return false;
        }
        if (!smsEnabled) {
            log.warn("SMS disabled. OTP fallback required. Target: {}", phoneNumber);
            return false;
        }

        // Provider integration point (MSG91/Twilio/etc). For now, we keep a safe local implementation.
        if ("log".equalsIgnoreCase(smsProvider)) {
            log.info("OTP SMS (dev-provider=log) to {}: {}", phoneNumber, messageBody);
            return true;
        }

        if ("msg91".equalsIgnoreCase(smsProvider)) {
            return sendViaMsg91(phoneNumber, messageBody);
        }
        if ("twilio".equalsIgnoreCase(smsProvider)) {
            return sendViaTwilio(phoneNumber, messageBody);
        }

        log.warn("Unsupported SMS provider '{}'. OTP fallback required for {}", smsProvider, phoneNumber);
        return false;
    }

    private boolean sendViaMsg91(String phoneNumber, String messageBody) {
        if (isBlank(msg91AuthKey) || isBlank(msg91SenderId)) {
            log.warn("MSG91 config missing auth key/sender id. OTP fallback required for {}", phoneNumber);
            return false;
        }
        String digits = phoneNumber.replaceAll("\\D", "");
        if (digits.startsWith("91") && digits.length() == 12) {
            // keep as is
        } else if (digits.length() == 10) {
            digits = "91" + digits;
        } else {
            log.warn("MSG91 send skipped. Invalid phone format: {}", phoneNumber);
            return false;
        }

        try {
            URI uri = UriComponentsBuilder.fromHttpUrl(msg91BaseUrl)
                    .queryParam("authkey", msg91AuthKey)
                    .queryParam("mobiles", digits)
                    .queryParam("message", messageBody)
                    .queryParam("sender", msg91SenderId)
                    .queryParam("route", msg91Route)
                    .queryParam("country", "91")
                    .build(true)
                    .toUri();

            ResponseEntity<String> response = restTemplate.getForEntity(uri, String.class);
            String body = response.getBody() == null ? "" : response.getBody().toLowerCase(Locale.ROOT);
            boolean ok = response.getStatusCode().is2xxSuccessful() && !body.contains("error");
            if (!ok) {
                log.warn("MSG91 send failed (status={}): {}", response.getStatusCode(), response.getBody());
            }
            return ok;
        } catch (Exception ex) {
            log.warn("MSG91 send exception for {}: {}", phoneNumber, ex.getMessage());
            return false;
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private boolean sendViaTwilio(String phoneNumber, String messageBody) {
        if (isBlank(twilioAccountSid) || isBlank(twilioAuthToken) || isBlank(twilioFromNumber)) {
            log.warn("Twilio config missing SID/auth token/from number. OTP fallback required for {}", phoneNumber);
            return false;
        }
        try {
            String to = normalizeToE164(phoneNumber);
            if (to == null) {
                log.warn("Twilio send skipped. Invalid phone format: {}", phoneNumber);
                return false;
            }

            URI uri = UriComponentsBuilder.fromHttpUrl(twilioBaseUrl)
                    .pathSegment("Accounts", twilioAccountSid, "Messages.json")
                    .build(true)
                    .toUri();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            headers.set("Authorization", basicAuth(twilioAccountSid, twilioAuthToken));

            String form = "To=" + urlEncode(to)
                    + "&From=" + urlEncode(twilioFromNumber)
                    + "&Body=" + urlEncode(messageBody);
            HttpEntity<String> entity = new HttpEntity<>(form, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(uri, entity, String.class);
            boolean ok = response.getStatusCode().is2xxSuccessful();
            if (!ok) {
                log.warn("Twilio send failed (status={}): {}", response.getStatusCode(), response.getBody());
            }
            return ok;
        } catch (Exception ex) {
            log.warn("Twilio send exception for {}: {}", phoneNumber, ex.getMessage());
            return false;
        }
    }

    private String normalizeToE164(String phoneNumber) {
        String digits = phoneNumber == null ? "" : phoneNumber.replaceAll("\\D", "");
        if (digits.startsWith("91") && digits.length() == 12) {
            return "+" + digits;
        }
        if (digits.length() == 10) {
            return "+91" + digits;
        }
        if (phoneNumber != null && phoneNumber.startsWith("+") && phoneNumber.length() >= 8) {
            return phoneNumber;
        }
        return null;
    }

    private String basicAuth(String sid, String token) {
        String raw = sid + ":" + token;
        return "Basic " + Base64.getEncoder().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    private String urlEncode(String value) {
        return java.net.URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }
}
