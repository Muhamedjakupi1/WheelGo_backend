package com.wheelGo.repository;

import com.wheelGo.model.booking_addons.BookingAddon;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface BookingAddonRepository extends JpaRepository<BookingAddon, UUID> {
    List<BookingAddon> findByBookingIdIn(List<UUID> bookingIds);
}
