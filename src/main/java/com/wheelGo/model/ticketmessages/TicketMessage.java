package com.wheelGo.model.ticketmessages;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table (name = "ticket_messages")
@Getter @Setter @NoArgsConstructor

public class TicketMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "ticket_id", nullable = false)
    private UUID ticketId;

    @Column (name = "sender_id", nullable = false)
    private UUID senderId;

    @Column (name = "message", nullable = false)
    private String message;

    @Column (name = "is_staff", nullable = false)
    private boolean isStaff = false;

    @Column (name = "sent_at", nullable = false)
    private LocalDateTime sentAt = LocalDateTime.now();
}
