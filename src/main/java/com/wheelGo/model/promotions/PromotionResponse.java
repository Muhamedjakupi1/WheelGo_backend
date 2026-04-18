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

    public static PromotionResponse from(Promotion promotion) {
        PromotionResponse res = new PromotionResponse();
        res.setId(promotion.getId());
        res.setCode(promotion.getCode());
        res.setDiscountType(promotion.getDiscountType());
        res.setDiscountValue(promotion.getDiscountValue());
        res.setMaxUses(promotion.getMaxUses());
        res.setUsesCount(promotion.getUsesCount());
        res.setValidFrom(promotion.getValidFrom());
        res.setValidUntil(promotion.getValidUntil());
        res.setIsActive(promotion.getIsActive());
        res.setCreatedAt(promotion.getCreatedAt());
        res.setUpdatedAt(promotion.getUpdatedAt());
        return res;
    }
}