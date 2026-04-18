package com.wheelGo.model.payments;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
public class PaymentRequest {
    private UUID bookingId;
    private BigDecimal amount;
    private String currency;
    private String method;
    private String status;
    private String gatewayRef;
    private LocalDateTime paidAt;
}