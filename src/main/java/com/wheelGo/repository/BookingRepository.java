package com.wheelGo.repository;

import com.wheelGo.model.bookings.Booking;
import com.wheelGo.model.bookings.BookingResponse;
import com.wheelGo.model.enums.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface BookingRepository extends JpaRepository<Booking, UUID> {
    List<Booking> findAllByUserIdOrderByCreatedAtDesc(UUID userId);
    List<Booking> findAllByOrderByCreatedAtDesc();
    List<Booking> findAllByStatusInAndEndDateBefore(Collection<BookingStatus> statuses, LocalDateTime dateTime);
    List<Booking> findAllByVehicleIdAndStatusInOrderByEndDateAsc(
            UUID vehicleId,
            Collection<BookingStatus> statuses
    );
    List<Booking> findAllByVehicleIdAndStatusInAndStartDateLessThanAndEndDateGreaterThanOrderByEndDateAsc(
            UUID vehicleId,
            Collection<BookingStatus> statuses,
            LocalDateTime requestedEnd,
            LocalDateTime requestedStart
    );

    boolean existsByVehicleIdAndStatusInAndStartDateLessThanAndEndDateGreaterThan(
            UUID vehicleId,
            Collection<BookingStatus> statuses,
            LocalDateTime requestedEnd,
            LocalDateTime requestedStart
    );

    boolean existsByVehicleIdAndStatusInAndStartDateLessThanAndEndDateGreaterThanAndIdNot(
            UUID vehicleId,
            Collection<BookingStatus> statuses,
            LocalDateTime requestedEnd,
            LocalDateTime requestedStart,
            UUID id
    );

    @Query("SELECT b FROM Booking b " +
            "LEFT JOIN Vehicle v ON v.id = b.vehicleId " +
            "LEFT JOIN User u ON u.id = b.userId " +
            "LEFT JOIN Location l ON l.id = b.pickupLocationId " +
            "WHERE LOWER(COALESCE(v.make, '')) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(COALESCE(v.model, '')) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(COALESCE(v.plateNumber, '')) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(COALESCE(u.email, '')) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(COALESCE(l.name, '')) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(CAST(b.status AS string)) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(COALESCE(b.notes, '')) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(CAST(b.startDate AS string)) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(CAST(b.endDate AS string)) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "ORDER BY b.createdAt DESC")
    List<Booking> searchBookingsForAdmin(String keyword);

    @Query("SELECT b FROM Booking b " +
            "LEFT JOIN Vehicle v ON v.id = b.vehicleId " +
            "LEFT JOIN Location l ON l.id = b.pickupLocationId " +
            "WHERE b.userId = :userId AND (" +
            "LOWER(COALESCE(v.make, '')) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(COALESCE(v.model, '')) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(COALESCE(v.plateNumber, '')) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(COALESCE(l.name, '')) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(CAST(b.status AS string)) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(COALESCE(b.notes, '')) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(CAST(b.startDate AS string)) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(CAST(b.endDate AS string)) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
            "ORDER BY b.createdAt DESC")
    List<Booking> searchBookingsForUser(UUID userId, String keyword);
}
