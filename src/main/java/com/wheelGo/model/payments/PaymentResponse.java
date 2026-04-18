package com.wheelGo.model.payments;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
public class PaymentResponse {
    private UUID id;
    private UUID bookingId;
    private BigDecimal amount;
    private String currency;
    private String method;
    private String status;
    private String gatewayRef;
    private LocalDateTime paidAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static PaymentResponse from(Payment payment) {
        PaymentResponse res = new PaymentResponse();
        res.setId(payment.getId());
        res.setBookingId(payment.getBookingId());
        res.setAmount(payment.getAmount());
        res.setCurrency(payment.getCurrency());
        res.setMethod(payment.getMethod());
        res.setStatus(payment.getStatus());
        res.setGatewayRef(payment.getGatewayRef());
        res.setPaidAt(payment.getPaidAt());
        res.setCreatedAt(payment.getCreatedAt());
        res.setUpdatedAt(payment.getUpdatedAt());
        return res;
    }
}