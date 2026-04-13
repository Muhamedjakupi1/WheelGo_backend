package com.wheelGo.model.supportTickets;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter @Setter
public class SupportTicketResponse {
    private UUID id;
    private UUID userId;
    private UUID bookingId;
    private String subject;
    private String status;
    private String priority;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static SupportTicketResponse from (SupportTicket supportTicket) {
        SupportTicketResponse response = new SupportTicketResponse();
        response.setId(supportTicket.getId());
        response.setUserId(supportTicket.getUserId());
        response.setBookingId(supportTicket.getBookingId());
        response.setSubject(supportTicket.getSubject());
        response.setStatus(supportTicket.getStatus());
        response.setPriority(supportTicket.getPriority());
        response.setCreatedAt(supportTicket.getCreatedAt());
        response.setUpdatedAt(supportTicket.getUpdatedAt());
        return response;
    }
}
