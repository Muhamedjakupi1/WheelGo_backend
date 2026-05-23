package com.wheelGo.model.payments;

import com.wheelGo.model.enums.PaymentStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PaymentAdminUpdateRequest {
    @NotNull(message = "Payment status is required")
    private PaymentStatus status;
}
