package com.wheelGo.model.promotions;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
public class PromotionResponse {
    private UUID id;
    private String code;
    private String discountType;
    private BigDecimal discountValue;
    private Integer maxUses;
    private Integer usesCount;
    private LocalDateTime validFrom;
    private LocalDateTime validUntil;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}