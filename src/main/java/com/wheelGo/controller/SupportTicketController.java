package com.wheelGo.controller;

import com.wheelGo.model.support_tickets.SupportTicketRequest;
import com.wheelGo.model.support_tickets.SupportTicketResponse;
import com.wheelGo.model.ticket_messages.TicketMessageRequest;
import com.wheelGo.model.ticket_messages.TicketMessageResponse;
import com.wheelGo.service.SupportTicketService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/support/tickets")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('USER', 'ADMIN', 'SUPER_ADMIN')")
public class SupportTicketController {

    private final SupportTicketService supportTicketService;

    @GetMapping("/me")
    public ResponseEntity<List<SupportTicketResponse>> getMyTickets() {
        return ResponseEntity.ok(supportTicketService.getMyTickets());
    }

    @PostMapping
    public ResponseEntity<SupportTicketResponse> create(@RequestBody @Valid SupportTicketRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(supportTicketService.createForCurrentUser(request));
    }

    @GetMapping("/{ticketId}/messages")
    public ResponseEntity<List<TicketMessageResponse>> getMessages(@PathVariable UUID ticketId) {
        return ResponseEntity.ok(supportTicketService.getMyMessages(ticketId));
    }

    @PostMapping("/{ticketId}/messages")
    public ResponseEntity<TicketMessageResponse> addMessage(@PathVariable UUID ticketId,
                                                            @RequestBody @Valid TicketMessageRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(supportTicketService.addUserMessage(ticketId, request));
    }
}
