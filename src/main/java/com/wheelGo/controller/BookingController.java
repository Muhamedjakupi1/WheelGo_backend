package com.wheelGo.controller;

import com.wheelGo.model.bookings.Booking;
import com.wheelGo.model.bookings.BookingCreateRequest;
import com.wheelGo.model.bookings.BookingResponse;
import com.wheelGo.service.BookingService;
import com.wheelGo.tools.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/bookings")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('USER', 'ADMIN', 'SUPER_ADMIN')")
public class BookingController {
    private final BookingService bookingService;

    @PostMapping
    public ResponseEntity<BookingResponse> createBooking(@RequestBody @Valid BookingCreateRequest request) {
        UUID userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.status(HttpStatus.CREATED).body(bookingService.createBooking(userId, request));
    }

    @GetMapping("/me")
    public ResponseEntity<List<BookingResponse>> getMyBookings(
            @RequestParam(value = "keyword", required = false) String keyword) {
        UUID userId = SecurityUtils.getCurrentUserId();
        if (keyword == null || keyword.trim().isEmpty()) {
            return ResponseEntity.ok(bookingService.getBookingsForUser(userId));
        }
        return ResponseEntity.ok(bookingService.getBookingsForUser(userId,keyword));
    }

    @PatchMapping("/{id}/cancel")
    public ResponseEntity<BookingResponse> cancelMyBooking(@PathVariable UUID id) {
        UUID userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(bookingService.cancelBooking(userId, id));
    }
}
