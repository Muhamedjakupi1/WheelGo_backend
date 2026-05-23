package com.wheelGo.service;

import com.wheelGo.mapper.ReviewMapper;
import com.wheelGo.model.bookings.Booking;
import com.wheelGo.model.enums.BookingStatus;
import com.wheelGo.model.reviews.Review;
import com.wheelGo.model.reviews.ReviewRequest;
import com.wheelGo.model.reviews.ReviewResponse;
import com.wheelGo.model.user.User;
import com.wheelGo.model.vehicles.Vehicle;
import com.wheelGo.repository.BookingRepository;
import com.wheelGo.repository.ReviewRepository;
import com.wheelGo.repository.UserRepository;
import com.wheelGo.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReviewService {
    private final ReviewRepository reviewRepository;
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final VehicleRepository vehicleRepository;
    private final ReviewMapper reviewMapper;
    private final CacheInvalidationService cacheInvalidationService;

    @Transactional
    public ReviewResponse createReview(UUID userId, ReviewRequest request) {
        Booking booking = bookingRepository.findById(request.getBookingId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Booking not found"));

        if (!booking.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You cannot review this booking");
        }

        if (booking.getStatus() != BookingStatus.COMPLETED || !Boolean.TRUE.equals(booking.getReviewEligible())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "This booking is not ready for review yet");
        }

        if (booking.getReviewSubmittedAt() != null || reviewRepository.existsByBookingId(booking.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A review was already submitted for this booking");
        }

        if (request.getVehicleId() != null && !booking.getVehicleId().equals(request.getVehicleId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Review vehicle does not match the booking");
        }

        Review review = new Review();
        review.setBookingId(booking.getId());
        review.setUserId(userId);
        review.setVehicleId(booking.getVehicleId());
        review.setRating(request.getRating());
        review.setComment(normalizeOptionalText(request.getComment()));
        review.setCreatedAt(LocalDateTime.now());

        Review saved = reviewRepository.save(review);
        booking.setReviewSubmittedAt(saved.getCreatedAt());
        booking.setReviewEligible(false);
        booking.setUpdatedAt(LocalDateTime.now());
        bookingRepository.save(booking);
        cacheInvalidationService.evictBookings(userId);
        cacheInvalidationService.evictBookingsForAdmin();

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<ReviewResponse> getMyReviews(UUID userId) {
        return reviewRepository.findAllByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ReviewResponse> getReviewsForAdmin() {
        return reviewRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ReviewResponse> getReviewsForVehicle(UUID vehicleId) {
        if (!vehicleRepository.existsById(vehicleId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Vehicle not found");
        }

        return reviewRepository.findAllByVehicleIdOrderByCreatedAtDesc(vehicleId).stream()
                .map(this::toResponse)
                .toList();
    }

    private ReviewResponse toResponse(Review review) {
        ReviewResponse response = reviewMapper.toResponse(review);
        userRepository.findById(review.getUserId())
                .map(User::getEmail)
                .ifPresent(response::setCustomerEmail);
        vehicleRepository.findById(review.getVehicleId())
                .map(this::formatVehicleName)
                .ifPresent(response::setVehicleName);
        return response;
    }

    private String formatVehicleName(Vehicle vehicle) {
        return (vehicle.getMake() + " " + vehicle.getModel()).trim();
    }

    private String normalizeOptionalText(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
