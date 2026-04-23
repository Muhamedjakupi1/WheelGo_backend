package com.wheelGo.model.promotions;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Future;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter @Setter
public class PromotionUpdateRequest {
    @DecimalMin(value = "0.0", message = "Value must be positive")
    private BigDecimal discountValue;

    private Integer maxUses;

    @Future(message = "Expiry date must be in the future")
    private LocalDateTime validUntil;

    private Boolean isActive;
}