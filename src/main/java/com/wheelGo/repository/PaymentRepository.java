package com.wheelGo.repository;

import com.wheelGo.model.payments.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {
    List<Payment> findAllByOrderByCreatedAtDesc();

    List<Payment> findAllByBookingIdInOrderByCreatedAtDesc(Collection<UUID> bookingIds);

    List<Payment> findAllByBookingIdOrderByCreatedAtDesc(UUID bookingId);

    Optional<Payment> findTopByBookingIdOrderByCreatedAtDesc(UUID bookingId);

    @Query("SELECT p FROM Payment p " +
            "LEFT JOIN Booking b ON b.id = p.bookingId " +
            "LEFT JOIN User u ON u.id = b.userId " +
            "LEFT JOIN Vehicle v ON v.id = b.vehicleId " +
            "LEFT JOIN Invoice i ON i.bookingId = p.bookingId " +
            "WHERE LOWER(CAST(p.status AS string)) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(CAST(p.method AS string)) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(COALESCE(p.currency, '')) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(COALESCE(p.gatewayRef, '')) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(COALESCE(i.invoiceNumber, '')) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(COALESCE(u.email, '')) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(COALESCE(v.make, '')) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(COALESCE(v.model, '')) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(COALESCE(v.plateNumber, '')) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(CAST(p.amount AS string)) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "ORDER BY p.createdAt DESC")
    List<Payment> searchPaymentsForAdmin(String keyword);

    @Query("SELECT p FROM Payment p " +
            "LEFT JOIN Booking b ON b.id = p.bookingId " +
            "LEFT JOIN Vehicle v ON v.id = b.vehicleId " +
            "LEFT JOIN Invoice i ON i.bookingId = p.bookingId " +
            "WHERE b.userId = :userId AND (" +
            "LOWER(CAST(p.status AS string)) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(CAST(p.method AS string)) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(COALESCE(p.currency, '')) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(COALESCE(p.gatewayRef, '')) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(COALESCE(i.invoiceNumber, '')) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(COALESCE(v.make, '')) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(COALESCE(v.model, '')) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(COALESCE(v.plateNumber, '')) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(CAST(p.amount AS string)) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
            "ORDER BY p.createdAt DESC")
    List<Payment> searchPaymentsForUser(UUID userId, String keyword);
}
