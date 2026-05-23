package com.wheelGo.model.payments;

import com.wheelGo.model.enums.PaymentMethod;
import com.wheelGo.model.enums.PaymentStatus;
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
    private PaymentMethod method;
    private PaymentStatus status;
    private String gatewayRef;
    private String promotionCode;
    private LocalDateTime paidAt;
}
