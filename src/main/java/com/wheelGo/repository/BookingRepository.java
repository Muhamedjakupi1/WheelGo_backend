package com.wheelGo.repository;

import com.wheelGo.model.bookings.Booking;
import com.wheelGo.model.enums.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;

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
}
