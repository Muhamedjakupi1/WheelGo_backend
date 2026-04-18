package com.wheelGo.model.supportTickets;


import com.wheelGo.model.enums.TicketPriority;
import com.wheelGo.model.enums.TicketStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcType;
import org.hibernate.dialect.type.PostgreSQLEnumJdbcType;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "support_tickets")
@Getter @Setter @NoArgsConstructor
public class SupportTicket {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column (name = "user_id", nullable = false)
    private UUID userId;

    @Column (name = "booking_id")
    private UUID bookingId;

    @Column (name = "subject", nullable = false, length = 150)
    private String subject;

    @Column (name = "status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    private TicketStatus status = TicketStatus.OPEN;

    @Column (name = "priority", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    private TicketPriority priority = TicketPriority.NORMAL;

    @Column (name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column (name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
