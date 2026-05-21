package com.wheelGo.service;

import com.wheelGo.model.enums.Role;
import com.wheelGo.model.enums.TicketPriority;
import com.wheelGo.model.enums.TicketStatus;
import com.wheelGo.model.support_tickets.SupportTicket;
import com.wheelGo.model.support_tickets.SupportTicketRequest;
import com.wheelGo.model.support_tickets.SupportTicketResponse;
import com.wheelGo.model.support_tickets.SupportTicketUpdateRequest;
import com.wheelGo.model.ticket_messages.TicketMessage;
import com.wheelGo.model.ticket_messages.TicketMessageRequest;
import com.wheelGo.model.ticket_messages.TicketMessageResponse;
import com.wheelGo.model.user.User;
import com.wheelGo.repository.SupportTicketRepository;
import com.wheelGo.repository.TicketMessageRepository;
import com.wheelGo.repository.UserRepository;
import com.wheelGo.security.CustomUserPrincipal;
import com.wheelGo.tools.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SupportTicketService {

    private final SupportTicketRepository supportTicketRepository;
    private final TicketMessageRepository ticketMessageRepository;
    private final UserRepository userRepository;
    private final AdminAccessService adminAccessService;

    @Transactional(readOnly = true)
    public List<SupportTicketResponse> getMyTickets() {
        UUID userId = requireCurrentUserId();
        return toResponses(supportTicketRepository.findAllByUserIdOrderByUpdatedAtDesc(userId));
    }

    @Transactional(readOnly = true)
    public List<SupportTicketResponse> getAllForAdmin() {
        adminAccessService.requireCurrentTenantId();
        return toResponses(supportTicketRepository.findAllByOrderByUpdatedAtDesc());
    }

    @Transactional
    public SupportTicketResponse createForCurrentUser(SupportTicketRequest request) {
        UUID userId = requireCurrentUserId();
        SupportTicket ticket = new SupportTicket();
        ticket.setUserId(userId);
        ticket.setBookingId(request.getBookingId());
        ticket.setSubject(requiredTrimmed(request.getSubject(), "Subject is required"));
        ticket.setPriority(request.getPriority() != null ? request.getPriority() : TicketPriority.NORMAL);
        ticket.setStatus(TicketStatus.OPEN);
        SupportTicket savedTicket = supportTicketRepository.save(ticket);

        saveMessage(savedTicket, userId, request.getMessage(), false);
        return toResponse(savedTicket);
    }

    @Transactional(readOnly = true)
    public List<TicketMessageResponse> getMyMessages(UUID ticketId) {
        SupportTicket ticket = findTicket(ticketId);
        UUID userId = requireCurrentUserId();
        if (!ticket.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You cannot access this support request");
        }
        return toMessageResponses(ticketMessageRepository.findAllByTicketIdOrderBySentAtAsc(ticketId));
    }

    @Transactional(readOnly = true)
    public List<TicketMessageResponse> getMessagesForAdmin(UUID ticketId) {
        adminAccessService.requireCurrentTenantId();
        findTicket(ticketId);
        return toMessageResponses(ticketMessageRepository.findAllByTicketIdOrderBySentAtAsc(ticketId));
    }

    @Transactional
    public TicketMessageResponse addUserMessage(UUID ticketId, TicketMessageRequest request) {
        SupportTicket ticket = findTicket(ticketId);
        UUID userId = requireCurrentUserId();
        if (!ticket.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You cannot update this support request");
        }
        if (ticket.getStatus() == TicketStatus.CLOSED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "This support request is closed");
        }
        return toMessageResponse(saveMessage(ticket, userId, request.getMessage(), false));
    }

    @Transactional
    public TicketMessageResponse addAdminMessage(UUID ticketId, TicketMessageRequest request) {
        SupportTicket ticket = findTicket(ticketId);
        CustomUserPrincipal principal = adminAccessService.requireCurrentPrincipal();
        if (!Role.ADMIN.name().equals(principal.getRole()) && !Role.SUPER_ADMIN.name().equals(principal.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only staff can reply");
        }
        if (ticket.getStatus() == TicketStatus.CLOSED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Reopen this support request before replying");
        }
        if (ticket.getStatus() == TicketStatus.OPEN) {
            ticket.setStatus(TicketStatus.IN_PROGRESS);
        }
        return toMessageResponse(saveMessage(ticket, principal.getUserId(), request.getMessage(), true));
    }

    @Transactional
    public SupportTicketResponse updateAsAdmin(UUID ticketId, SupportTicketUpdateRequest request) {
        adminAccessService.requireCurrentTenantId();
        SupportTicket ticket = findTicket(ticketId);
        if (request.getSubject() != null && !request.getSubject().isBlank()) {
            ticket.setSubject(request.getSubject().trim());
        }
        if (request.getStatus() != null && !request.getStatus().isBlank()) {
            ticket.setStatus(TicketStatus.valueOf(request.getStatus().trim().toUpperCase()));
        }
        if (request.getPriority() != null && !request.getPriority().isBlank()) {
            ticket.setPriority(TicketPriority.valueOf(request.getPriority().trim().toUpperCase()));
        }
        ticket.setUpdatedAt(LocalDateTime.now());
        return toResponse(supportTicketRepository.save(ticket));
    }

    private TicketMessage saveMessage(SupportTicket ticket, UUID senderId, String message, boolean isStaff) {
        TicketMessage ticketMessage = new TicketMessage();
        ticketMessage.setTicketId(ticket.getId());
        ticketMessage.setSenderId(senderId);
        ticketMessage.setMessage(requiredTrimmed(message, "Message is required"));
        ticketMessage.setStaff(isStaff);
        TicketMessage savedMessage = ticketMessageRepository.save(ticketMessage);

        ticket.setUpdatedAt(LocalDateTime.now());
        supportTicketRepository.save(ticket);
        return savedMessage;
    }

    private SupportTicket findTicket(UUID ticketId) {
        return supportTicketRepository.findById(ticketId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Support request not found"));
    }

    private UUID requireCurrentUserId() {
        UUID userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authenticated user context is missing");
        }
        return userId;
    }

    private List<SupportTicketResponse> toResponses(List<SupportTicket> tickets) {
        if (tickets.isEmpty()) {
            return List.of();
        }
        Map<UUID, User> usersById = userRepository.findAllById(
                tickets.stream().map(SupportTicket::getUserId).distinct().toList()
        ).stream().collect(Collectors.toMap(User::getId, Function.identity()));

        return tickets.stream()
                .map(ticket -> toResponse(ticket, usersById.get(ticket.getUserId())))
                .toList();
    }

    private SupportTicketResponse toResponse(SupportTicket ticket) {
        User user = userRepository.findById(ticket.getUserId()).orElse(null);
        return toResponse(ticket, user);
    }

    private SupportTicketResponse toResponse(SupportTicket ticket, User user) {
        SupportTicketResponse response = new SupportTicketResponse();
        response.setId(ticket.getId());
        response.setUserId(ticket.getUserId());
        response.setBookingId(ticket.getBookingId());
        response.setCustomerEmail(user != null ? user.getEmail() : null);
        response.setSubject(ticket.getSubject());
        response.setStatus(ticket.getStatus());
        response.setPriority(ticket.getPriority());
        response.setCreatedAt(ticket.getCreatedAt());
        response.setUpdatedAt(ticket.getUpdatedAt());
        ticketMessageRepository.findAllByTicketIdOrderBySentAtAsc(ticket.getId()).stream()
                .reduce((first, second) -> second)
                .ifPresent(message -> response.setLastMessage(message.getMessage()));
        return response;
    }

    private List<TicketMessageResponse> toMessageResponses(List<TicketMessage> messages) {
        return messages.stream().map(this::toMessageResponse).toList();
    }

    private TicketMessageResponse toMessageResponse(TicketMessage message) {
        TicketMessageResponse response = new TicketMessageResponse();
        response.setId(message.getId());
        response.setTicketId(message.getTicketId());
        response.setSenderId(message.getSenderId());
        response.setMessage(message.getMessage());
        response.setStaff(message.isStaff());
        response.setSentAt(message.getSentAt());
        return response;
    }

    private String requiredTrimmed(String value, String message) {
        if (value == null || value.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }
        return value.trim();
    }
}
