package com.wheelGo.repository;

import com.wheelGo.model.ticket_messages.TicketMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TicketMessageRepository extends JpaRepository<TicketMessage, UUID> {
    List<TicketMessage> findAllByTicketIdOrderBySentAtAsc(UUID ticketId);
}
