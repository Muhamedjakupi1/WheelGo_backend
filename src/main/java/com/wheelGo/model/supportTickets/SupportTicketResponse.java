package com.wheelGo.model.supportTickets;

import com.wheelGo.model.enums.TicketPriority;
import com.wheelGo.model.enums.TicketStatus;
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
    private TicketStatus status;
    private TicketPriority priority;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
