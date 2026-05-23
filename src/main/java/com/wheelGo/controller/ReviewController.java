package com.wheelGo.controller;

import com.wheelGo.model.reviews.ReviewRequest;
import com.wheelGo.model.reviews.ReviewResponse;
import com.wheelGo.service.ReviewService;
import com.wheelGo.tools.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/reviews")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('USER', 'ADMIN', 'SUPER_ADMIN')")
public class ReviewController {
    private final ReviewService reviewService;

    @PostMapping
    public ResponseEntity<ReviewResponse> createReview(@RequestBody @Valid ReviewRequest request) {
        UUID userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.status(HttpStatus.CREATED).body(reviewService.createReview(userId, request));
    }

    @GetMapping("/me")
    public ResponseEntity<List<ReviewResponse>> getMyReviews() {
        UUID userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(reviewService.getMyReviews(userId));
    }

    @GetMapping("/vehicles/{vehicleId}")
    public ResponseEntity<List<ReviewResponse>> getVehicleReviews(@PathVariable UUID vehicleId) {
        return ResponseEntity.ok(reviewService.getReviewsForVehicle(vehicleId));
    }
}
