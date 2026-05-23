package com.wheelGo.controller;

import com.wheelGo.model.payments.PaymentAdminUpdateRequest;
import com.wheelGo.model.payments.PaymentResponse;
import com.wheelGo.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/payments")
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
@RequiredArgsConstructor
public class PaymentAdminController {
    private final PaymentService paymentService;

    @GetMapping
    public ResponseEntity<List<PaymentResponse>> getAll(
            @RequestParam(value = "keyword", required = false) String keyword) {
        return ResponseEntity.ok(paymentService.getPaymentsForAdmin(keyword));
    }

    @PatchMapping("/{id}/confirm")
    public ResponseEntity<PaymentResponse> confirmCashPayment(@PathVariable UUID id) {
        return ResponseEntity.ok(paymentService.confirmCashPayment(id));
    }

    @PatchMapping("/{id}/refund")
    public ResponseEntity<PaymentResponse> refundPayment(@PathVariable UUID id) {
        return ResponseEntity.ok(paymentService.refundPayment(id));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<PaymentResponse> updateStatus(@PathVariable UUID id,
                                                        @RequestBody @Valid PaymentAdminUpdateRequest request) {
        return ResponseEntity.ok(paymentService.updatePaymentStatus(id, request));
    }
}
