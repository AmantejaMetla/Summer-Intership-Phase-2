package com.eshop.user.service;

import com.eshop.user.entity.SupportMessage;
import com.eshop.user.entity.SupportMessage.ChannelType;
import com.eshop.user.entity.SupportMessage.SenderType;
import com.eshop.user.entity.SupportTicket;
import com.eshop.user.entity.SupportTicket.TicketStatus;
import com.eshop.user.repository.SupportMessageRepository;
import com.eshop.user.repository.SupportTicketRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class SupportTicketService {
    private static final Pattern TICKET_ID_PATTERN = Pattern.compile("(?i)(?:ticket\\s*#?\\s*|#)(\\d+)");

    private final SupportTicketRepository supportTicketRepository;
    private final SupportMessageRepository supportMessageRepository;
    private final WhatsAppBridgeService whatsAppBridgeService;

    public SupportTicketService(SupportTicketRepository supportTicketRepository,
                                SupportMessageRepository supportMessageRepository,
                                WhatsAppBridgeService whatsAppBridgeService) {
        this.supportTicketRepository = supportTicketRepository;
        this.supportMessageRepository = supportMessageRepository;
        this.whatsAppBridgeService = whatsAppBridgeService;
    }

    public SupportTicket create(
            Long userId,
            String fullName,
            String email,
            String phone,
            String orderId,
            String subject,
            String message,
            String screenshotName
    ) {
        SupportTicket ticket = new SupportTicket();
        ticket.setUserId(userId);
        ticket.setFullName(fullName);
        ticket.setEmail(email);
        ticket.setPhone(phone);
        ticket.setOrderId(orderId);
        ticket.setSubject(subject);
        ticket.setMessage(message);
        ticket.setScreenshotName(screenshotName);
        ticket.setStatus(TicketStatus.OPEN);
        SupportTicket saved = supportTicketRepository.save(ticket);
        saveMessage(saved.getId(), userId, SenderType.USER, ChannelType.WEB, message, null);
        whatsAppBridgeService.notifyNewTicket(saved);
        return saved;
    }

    public List<SupportTicket> listMine(Long userId) {
        return supportTicketRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public List<SupportTicket> listAll() {
        return supportTicketRepository.findAll()
                .stream()
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .toList();
    }

    public Optional<SupportTicket> updateStatus(Long id, TicketStatus status, String adminNotes) {
        return supportTicketRepository.findById(id).map(ticket -> {
            if (status != null) ticket.setStatus(status);
            if (adminNotes != null) ticket.setAdminNotes(adminNotes);
            return supportTicketRepository.save(ticket);
        });
    }

    public Optional<SupportTicket> findById(Long id) {
        return supportTicketRepository.findById(id);
    }

    public List<SupportMessage> listMessages(Long ticketId) {
        return supportMessageRepository.findByTicketIdOrderByCreatedAtAsc(ticketId);
    }

    public Optional<SupportMessage> addUserMessage(Long ticketId, Long userId, String body) {
        return supportTicketRepository.findById(ticketId)
                .filter(t -> t.getUserId().equals(userId))
                .map(t -> {
                    SupportMessage saved = saveMessage(ticketId, userId, SenderType.USER, ChannelType.WEB, body, null);
                    whatsAppBridgeService.forwardUserMessageToSupport(t, body);
                    return saved;
                });
    }

    public Optional<SupportMessage> addAdminMessage(Long ticketId, Long adminUserId, String body) {
        return supportTicketRepository.findById(ticketId)
                .map(t -> {
                    SupportMessage saved = saveMessage(ticketId, adminUserId, SenderType.ADMIN, ChannelType.WEB, body, null);
                    whatsAppBridgeService.forwardAdminMessageToCustomer(t, body);
                    return saved;
                });
    }

    public Optional<SupportMessage> ingestInboundWhatsApp(String fromNumber, String body, String externalMessageId) {
        String normalizedFrom = whatsAppBridgeService.normalizePhone(fromNumber);
        if (normalizedFrom == null) return Optional.empty();

        String supportNumber = whatsAppBridgeService.getSupportNumberNormalized();
        if (supportNumber != null && supportNumber.equals(normalizedFrom)) {
            Long ticketId = extractTicketId(body);
            if (ticketId == null) return Optional.empty();
            return supportTicketRepository.findById(ticketId).map(ticket -> {
                String cleanBody = stripTicketPrefix(body);
                SupportMessage saved = saveMessage(ticket.getId(), 0L, SenderType.WHATSAPP_SUPPORT, ChannelType.WHATSAPP, cleanBody, externalMessageId);
                whatsAppBridgeService.forwardAdminMessageToCustomer(ticket, cleanBody);
                return saved;
            });
        }

        return supportTicketRepository.findTopByPhoneOrderByCreatedAtDesc(normalizedFrom)
                .map(ticket -> saveMessage(ticket.getId(), 0L, SenderType.WHATSAPP_SUPPORT, ChannelType.WHATSAPP, body, externalMessageId));
    }

    private Long extractTicketId(String body) {
        if (body == null) return null;
        Matcher m = TICKET_ID_PATTERN.matcher(body);
        if (!m.find()) return null;
        try {
            return Long.valueOf(m.group(1));
        } catch (Exception e) {
            return null;
        }
    }

    private String stripTicketPrefix(String body) {
        if (body == null) return "";
        return body.replaceFirst("(?i)^(?:\\s*(?:ticket\\s*#?\\s*\\d+|#\\d+)\\s*[:\\-]?\\s*)", "").trim();
    }

    private SupportMessage saveMessage(Long ticketId, Long senderUserId, SenderType senderType, ChannelType channel, String body, String externalId) {
        SupportMessage msg = new SupportMessage();
        msg.setTicketId(ticketId);
        msg.setSenderUserId(senderUserId);
        msg.setSenderType(senderType);
        msg.setChannel(channel);
        msg.setBody(body);
        msg.setExternalMessageId(externalId);
        return supportMessageRepository.save(msg);
    }
}
