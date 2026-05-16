package com.wheelGo.controller;

import com.wheelGo.model.bookings.BookingAdminDecisionRequest;
import com.wheelGo.model.bookings.BookingResponse;
import com.wheelGo.service.BookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/bookings")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
public class BookingAdminController {
    private final BookingService bookingService;

    @GetMapping
    public ResponseEntity<List<BookingResponse>> getAll() {
        return ResponseEntity.ok(bookingService.getBookingsForAdmin());
    }

    @PatchMapping("/{id}/confirm")
    public ResponseEntity<BookingResponse> confirm(@PathVariable UUID id,
                                                   @RequestBody @Valid BookingAdminDecisionRequest request) {
        return ResponseEntity.ok(bookingService.confirmBooking(id, request));
    }

    @PatchMapping("/{id}/reject")
    public ResponseEntity<BookingResponse> reject(@PathVariable UUID id,
                                                  @RequestBody(required = false) BookingAdminDecisionRequest request) {
        return ResponseEntity.ok(bookingService.rejectBooking(id, request));
    }
}
