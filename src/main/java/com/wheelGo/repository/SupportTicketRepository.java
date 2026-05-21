package com.wheelGo.repository;

import com.wheelGo.model.support_tickets.SupportTicket;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SupportTicketRepository extends JpaRepository<SupportTicket, UUID> {
    List<SupportTicket> findAllByOrderByUpdatedAtDesc();
    List<SupportTicket> findAllByUserIdOrderByUpdatedAtDesc(UUID userId);
}
