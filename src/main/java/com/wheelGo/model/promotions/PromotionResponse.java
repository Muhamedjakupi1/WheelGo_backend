package com.wheelGo.model.promotions;

import com.wheelGo.model.enums.DiscountType;
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
    private DiscountType discountType;
    private BigDecimal discountValue;
    private Integer maxUses;
    private Integer usesCount;
    private LocalDateTime validFrom;
    private LocalDateTime validUntil;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}