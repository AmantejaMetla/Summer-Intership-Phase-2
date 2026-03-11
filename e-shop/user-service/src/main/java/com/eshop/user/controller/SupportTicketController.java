package com.eshop.user.controller;

import com.eshop.user.entity.SupportMessage;
import com.eshop.user.entity.SupportTicket;
import com.eshop.user.entity.SupportTicket.TicketStatus;
import com.eshop.user.service.WhatsAppBridgeService;
import com.eshop.user.service.SupportTicketService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/support")
public class SupportTicketController {

    private final SupportTicketService supportTicketService;
    private final WhatsAppBridgeService whatsAppBridgeService;

    public SupportTicketController(SupportTicketService supportTicketService,
                                   WhatsAppBridgeService whatsAppBridgeService) {
        this.supportTicketService = supportTicketService;
        this.whatsAppBridgeService = whatsAppBridgeService;
    }

    private static boolean hasRole(String rolesHeader, String role) {
        if (rolesHeader == null || rolesHeader.isBlank()) return false;
        return Arrays.stream(rolesHeader.split(","))
                .map(String::trim)
                .anyMatch(r -> role.equalsIgnoreCase(r));
    }

    @PostMapping("/tickets")
    public ResponseEntity<SupportTicket> create(
            @RequestHeader("X-User-Id") Long userId,
            @RequestBody CreateTicketRequest request) {
        if (request.fullName() == null || request.fullName().isBlank()
                || request.email() == null || request.email().isBlank()
                || request.subject() == null || request.subject().isBlank()
                || request.message() == null || request.message().isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        SupportTicket ticket = supportTicketService.create(
                userId,
                request.fullName(),
                request.email(),
                request.phone(),
                request.orderId(),
                request.subject(),
                request.message(),
                request.screenshotName()
        );
        return ResponseEntity.ok(ticket);
    }

    @GetMapping("/tickets/me")
    public List<SupportTicket> myTickets(@RequestHeader("X-User-Id") Long userId) {
        return supportTicketService.listMine(userId);
    }

    @GetMapping("/tickets/admin")
    public ResponseEntity<List<SupportTicket>> adminTickets(
            @RequestHeader(value = "X-Roles", required = false) String rolesHeader) {
        if (!hasRole(rolesHeader, "admin")) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(supportTicketService.listAll());
    }

    @PatchMapping("/tickets/{id}")
    public ResponseEntity<SupportTicket> updateStatus(
            @PathVariable Long id,
            @RequestHeader(value = "X-Roles", required = false) String rolesHeader,
            @RequestBody UpdateTicketRequest request) {
        if (!hasRole(rolesHeader, "admin")) {
            return ResponseEntity.status(403).build();
        }
        TicketStatus status = null;
        if (request.status() != null && !request.status().isBlank()) {
            try {
                status = TicketStatus.valueOf(request.status().trim().toUpperCase());
            } catch (Exception ignored) {
                return ResponseEntity.badRequest().build();
            }
        }
        return supportTicketService.updateStatus(id, status, request.adminNotes())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/tickets/{id}/messages")
    public ResponseEntity<List<SupportMessage>> messages(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader(value = "X-Roles", required = false) String rolesHeader) {
        return supportTicketService.findById(id)
                .filter(t -> t.getUserId().equals(userId) || hasRole(rolesHeader, "admin"))
                .map(t -> ResponseEntity.ok(supportTicketService.listMessages(id)))
                .orElse(ResponseEntity.status(403).build());
    }

    @PostMapping("/tickets/{id}/messages")
    public ResponseEntity<SupportMessage> sendMessage(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader(value = "X-Roles", required = false) String rolesHeader,
            @RequestBody MessageRequest request) {
        if (request.body() == null || request.body().isBlank()) return ResponseEntity.badRequest().build();
        if (hasRole(rolesHeader, "admin")) {
            return supportTicketService.addAdminMessage(id, userId, request.body())
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        }
        return supportTicketService.addUserMessage(id, userId, request.body())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(403).build());
    }

    // Meta WhatsApp webhook verification
    @GetMapping("/whatsapp/webhook")
    public ResponseEntity<String> verifyWebhook(
            @RequestParam(value = "hub.mode", required = false) String mode,
            @RequestParam(value = "hub.verify_token", required = false) String verifyToken,
            @RequestParam(value = "hub.challenge", required = false) String challenge,
            @RequestParam(value = "hub_mode", required = false) String modeFallback,
            @RequestParam(value = "hub_verify_token", required = false) String tokenFallback,
            @RequestParam(value = "hub_challenge", required = false) String challengeFallback,
            @RequestParam(value = "verify_token", required = false) String tokenLegacy
    ) {
        String m = mode != null ? mode : modeFallback;
        String v = verifyToken != null ? verifyToken : (tokenFallback != null ? tokenFallback : tokenLegacy);
        String c = challenge != null ? challenge : challengeFallback;
        String expected = System.getenv().getOrDefault("META_WA_VERIFY_TOKEN", "");
        if ("subscribe".equals(m) && expected.equals(v) && c != null) {
            return ResponseEntity.ok(c);
        }
        return ResponseEntity.status(403).body("forbidden");
    }

    @PostMapping("/whatsapp/webhook")
    public ResponseEntity<?> inboundWebhook(@RequestBody Map<String, Object> payload) {
        WhatsAppBridgeService.WhatsAppInbound inbound = whatsAppBridgeService.parseInboundMessage(payload);
        if (inbound == null) return ResponseEntity.ok(Map.of("status", "ignored"));
        supportTicketService.ingestInboundWhatsApp(inbound.fromNumber(), inbound.body(), inbound.messageId());
        return ResponseEntity.ok(Map.of("status", "received"));
    }

    public record CreateTicketRequest(
            String fullName,
            String email,
            String phone,
            String orderId,
            String subject,
            String message,
            String screenshotName
    ) {}

    public record MessageRequest(String body) {}
    public record UpdateTicketRequest(String status, String adminNotes) {}
}
