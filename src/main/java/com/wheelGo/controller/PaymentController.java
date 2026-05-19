package com.wheelGo.controller;

import com.wheelGo.model.payments.PaymentRequest;
import com.wheelGo.model.payments.PaymentResponse;
import com.wheelGo.service.PaymentService;
import com.wheelGo.tools.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
@RequestMapping("/api/v1/payments")
@PreAuthorize("hasAnyRole('USER', 'ADMIN', 'SUPER_ADMIN')")
@RequiredArgsConstructor
public class PaymentController {
    private final PaymentService paymentService;

    @PostMapping("/pay")
    public ResponseEntity<PaymentResponse> pay(@Valid @RequestBody PaymentRequest request) {
        UUID userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(paymentService.payForBooking(userId, request));
    }

    @GetMapping("/me")
    public ResponseEntity<List<PaymentResponse>> getMyPayments() {
        UUID userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(paymentService.getPaymentsForUser(userId));
    }

    @GetMapping("/booking/{bookingId}")
    public ResponseEntity<PaymentResponse> getLatestPaymentForBooking(@PathVariable UUID bookingId) {
        UUID userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(paymentService.getLatestPaymentForBooking(userId, bookingId));
    }
}
