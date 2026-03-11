package com.eshop.user.service;

import com.eshop.user.entity.SupportTicket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
public class WhatsAppBridgeService {
    private static final Logger log = LoggerFactory.getLogger(WhatsAppBridgeService.class);

    @Value("${support.whatsapp.provider:none}")
    private String provider;
    @Value("${support.whatsapp.meta.phone-number-id:}")
    private String phoneNumberId;
    @Value("${support.whatsapp.meta.access-token:}")
    private String accessToken;
    @Value("${support.whatsapp.support-number:}")
    private String supportNumber;

    private final RestTemplate restTemplate = new RestTemplate();

    public void notifyNewTicket(SupportTicket ticket) {
        String content = "New E-Shop support ticket #" + ticket.getId()
                + "\nSubject: " + ticket.getSubject()
                + "\nFrom: " + ticket.getFullName() + " (" + ticket.getEmail() + ")"
                + "\nMessage: " + ticket.getMessage();
        sendTextToNumber(supportNumber, content, "new ticket #" + ticket.getId());
    }

    public void forwardUserMessageToSupport(SupportTicket ticket, String message) {
        String content = "Ticket #" + ticket.getId() + " | USER"
                + "\nFrom: " + ticket.getFullName()
                + "\nMessage: " + message;
        sendTextToNumber(supportNumber, content, "user message ticket #" + ticket.getId());
    }

    public void forwardAdminMessageToCustomer(SupportTicket ticket, String message) {
        String normalizedCustomer = normalizePhone(ticket.getPhone());
        if (normalizedCustomer == null || normalizedCustomer.isBlank()) {
            log.info("Ticket #{} has no customer phone; skipping outbound WhatsApp reply.", ticket.getId());
            return;
        }
        String content = "E-Shop Support (Ticket #" + ticket.getId() + ")\n" + message;
        sendTextToNumber(normalizedCustomer, content, "admin reply ticket #" + ticket.getId());
    }

    private void sendTextToNumber(String toNumber, String content, String logContext) {
        if (!"meta".equalsIgnoreCase(provider)) {
            log.info("WhatsApp provider disabled or not meta; skipped {}", logContext);
            return;
        }
        String normalizedTo = normalizePhone(toNumber);
        if (phoneNumberId == null || phoneNumberId.isBlank() || accessToken == null || accessToken.isBlank() || normalizedTo == null || normalizedTo.isBlank()) {
            log.info("WhatsApp Meta not fully configured; skipped {}", logContext);
            return;
        }

        String url = "https://graph.facebook.com/v20.0/" + phoneNumberId + "/messages";
        Map<String, Object> textObj = Map.of("preview_url", false, "body", content);
        Map<String, Object> body = new HashMap<>();
        body.put("messaging_product", "whatsapp");
        body.put("to", normalizedTo);
        body.put("type", "text");
        body.put("text", textObj);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        try {
            restTemplate.postForEntity(url, new HttpEntity<>(body, headers), String.class);
            log.info("Forwarded WhatsApp message: {}", logContext);
        } catch (Exception e) {
            log.warn("Failed WhatsApp message ({}): {}", logContext, e.getMessage());
        }
    }

    public String normalizePhone(String raw) {
        if (raw == null) return null;
        String digits = raw.replaceAll("[^0-9]", "");
        if (digits.isBlank()) return null;
        if (digits.startsWith("0")) digits = digits.substring(1);
        if (digits.length() == 10) digits = "91" + digits;
        return digits;
    }

    public String getSupportNumberNormalized() {
        return normalizePhone(supportNumber);
    }

    @SuppressWarnings("unchecked")
    public WhatsAppInbound parseInboundMessage(Map<String, Object> payload) {
        try {
            var entry = ((java.util.List<Map<String, Object>>) payload.get("entry")).get(0);
            var changes = ((java.util.List<Map<String, Object>>) entry.get("changes")).get(0);
            var value = (Map<String, Object>) changes.get("value");
            var messages = (java.util.List<Map<String, Object>>) value.get("messages");
            if (messages == null || messages.isEmpty()) return null;
            var msg = messages.get(0);
            var textObj = (Map<String, Object>) msg.get("text");
            String body = textObj != null ? String.valueOf(textObj.get("body")) : null;
            String from = msg.get("from") != null ? String.valueOf(msg.get("from")) : null;
            String id = msg.get("id") != null ? String.valueOf(msg.get("id")) : null;
            if (body == null || from == null) return null;
            return new WhatsAppInbound(from, body, id);
        } catch (Exception e) {
            return null;
        }
    }

    public record WhatsAppInbound(String fromNumber, String body, String messageId) {}
}
