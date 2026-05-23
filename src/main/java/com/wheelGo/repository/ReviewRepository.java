package com.wheelGo.repository;

import com.wheelGo.model.reviews.Review;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReviewRepository extends JpaRepository<Review, UUID> {
    boolean existsByBookingId(UUID bookingId);
    Optional<Review> findByBookingId(UUID bookingId);
    List<Review> findAllByOrderByCreatedAtDesc();
    List<Review> findAllByUserIdOrderByCreatedAtDesc(UUID userId);
    List<Review> findAllByVehicleIdOrderByCreatedAtDesc(UUID vehicleId);
}
