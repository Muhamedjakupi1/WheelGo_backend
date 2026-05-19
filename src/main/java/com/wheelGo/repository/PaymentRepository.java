package com.wheelGo.repository;

import com.wheelGo.model.payments.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {
    List<Payment> findAllByOrderByCreatedAtDesc();

    List<Payment> findAllByBookingIdInOrderByCreatedAtDesc(Collection<UUID> bookingIds);

    List<Payment> findAllByBookingIdOrderByCreatedAtDesc(UUID bookingId);

    Optional<Payment> findTopByBookingIdOrderByCreatedAtDesc(UUID bookingId);
}
