package com.wheelGo.controller;

import com.wheelGo.model.support_tickets.SupportTicketResponse;
import com.wheelGo.model.support_tickets.SupportTicketUpdateRequest;
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
@RequestMapping("/api/v1/admin/support/tickets")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
public class SupportTicketAdminController {

    private final SupportTicketService supportTicketService;

    @GetMapping
    public ResponseEntity<List<SupportTicketResponse>> getAll() {
        return ResponseEntity.ok(supportTicketService.getAllForAdmin());
    }

    @PatchMapping("/{ticketId}")
    public ResponseEntity<SupportTicketResponse> update(@PathVariable UUID ticketId,
                                                        @RequestBody SupportTicketUpdateRequest request) {
        return ResponseEntity.ok(supportTicketService.updateAsAdmin(ticketId, request));
    }

    @GetMapping("/{ticketId}/messages")
    public ResponseEntity<List<TicketMessageResponse>> getMessages(@PathVariable UUID ticketId) {
        return ResponseEntity.ok(supportTicketService.getMessagesForAdmin(ticketId));
    }

    @PostMapping("/{ticketId}/messages")
    public ResponseEntity<TicketMessageResponse> addMessage(@PathVariable UUID ticketId,
                                                            @RequestBody @Valid TicketMessageRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(supportTicketService.addAdminMessage(ticketId, request));
    }
}
