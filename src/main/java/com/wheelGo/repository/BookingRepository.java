package com.wheelGo.repository;

import com.wheelGo.model.bookings.Booking;
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
    @Query("""
            SELECT b
            FROM Booking b
            WHERE b.status = :status
              AND b.reviewSubmittedAt IS NULL
              AND (b.reviewEligible = false OR b.reviewEligible IS NULL)
            """)
    List<Booking> findAllReviewEligibilityCandidates(BookingStatus status);
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

    @Query(value = """
            SELECT b.*
            FROM bookings b
            LEFT JOIN vehicles v ON v.id = b.vehicle_id
            LEFT JOIN public.users u ON u.id = b.user_id
            LEFT JOIN locations l ON l.id = b.pickup_location_id
            WHERE LOWER(COALESCE(v.make, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(COALESCE(v.model, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(COALESCE(v.plate_number, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(COALESCE(u.email, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(COALESCE(l.name, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(CAST(b.status AS TEXT)) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(COALESCE(b.notes, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(CAST(b.start_date AS TEXT)) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(CAST(b.end_date AS TEXT)) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(CAST(b.total_price AS TEXT)) LIKE LOWER(CONCAT('%', :keyword, '%'))
            ORDER BY b.created_at DESC
            """, nativeQuery = true)
    List<Booking> searchBookingsForAdmin(String keyword);

    @Query(value = """
            SELECT b.*
            FROM bookings b
            LEFT JOIN vehicles v ON v.id = b.vehicle_id
            LEFT JOIN locations l ON l.id = b.pickup_location_id
            WHERE b.user_id = :userId
              AND (
                  LOWER(COALESCE(v.make, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(COALESCE(v.model, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(COALESCE(v.plate_number, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(COALESCE(l.name, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(CAST(b.status AS TEXT)) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(COALESCE(b.notes, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(CAST(b.start_date AS TEXT)) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(CAST(b.end_date AS TEXT)) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(CAST(b.total_price AS TEXT)) LIKE LOWER(CONCAT('%', :keyword, '%'))
              )
            ORDER BY b.created_at DESC
            """, nativeQuery = true)
    List<Booking> searchBookingsForUser(UUID userId, String keyword);
}
