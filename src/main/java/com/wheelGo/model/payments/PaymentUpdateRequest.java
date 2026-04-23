package com.wheelGo.model.payments;

import com.wheelGo.model.enums.PaymentStatus;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter @Setter
public class PaymentUpdateRequest {
    private PaymentStatus status;

    @Size(max = 150)
    private String gatewayRef;

    private LocalDateTime paidAt;
}