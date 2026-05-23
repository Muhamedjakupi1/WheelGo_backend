package com.wheelGo.service;

import com.wheelGo.model.enums.DiscountType;
import com.wheelGo.model.promotions.Promotion;
import com.wheelGo.model.promotions.PromotionRequest;
import com.wheelGo.model.promotions.PromotionResponse;
import com.wheelGo.repository.PromotionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PromotionAdminService {
    private final PromotionRepository promotionRepository;

    @Transactional(readOnly = true)
    public List<PromotionResponse> getAll() {
        return promotionRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public PromotionResponse create(PromotionRequest request) {
        String code = normalizeCode(request.getCode());
        if (promotionRepository.existsByCodeIgnoreCase(code)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A promotion with this code already exists");
        }

        Promotion promotion = new Promotion();
        promotion.setCode(code);
        promotion.setDiscountType(request.getDiscountType() != null ? request.getDiscountType() : DiscountType.PERCENTAGE);
        promotion.setDiscountValue(normalizeDiscountValue(promotion.getDiscountType(), request.getDiscountValue()));
        promotion.setMaxUses(validateMaxUses(request.getMaxUses()));
        promotion.setUsesCount(validateUsesCount(request.getUsesCount()));
        promotion.setValidFrom(request.getValidFrom() != null ? request.getValidFrom() : LocalDateTime.now());
        promotion.setValidUntil(request.getValidUntil());
        promotion.setIsActive(request.getIsActive() != null ? request.getIsActive() : true);
        promotion.setCreatedAt(LocalDateTime.now());
        promotion.setUpdatedAt(LocalDateTime.now());
        validateDateRange(promotion.getValidFrom(), promotion.getValidUntil());
        return toResponse(promotionRepository.save(promotion));
    }

    @Transactional
    public PromotionResponse update(UUID id, PromotionRequest request) {
        Promotion promotion = promotionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Promotion not found"));

        if (request.getCode() != null) {
            String code = normalizeCode(request.getCode());
            promotionRepository.findFirstByCodeIgnoreCase(code)
                    .filter(existing -> !existing.getId().equals(id))
                    .ifPresent(existing -> {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A promotion with this code already exists");
                    });
            promotion.setCode(code);
        }
        if (request.getDiscountType() != null) {
            promotion.setDiscountType(request.getDiscountType());
        }
        if (request.getDiscountValue() != null) {
            promotion.setDiscountValue(normalizeDiscountValue(promotion.getDiscountType(), request.getDiscountValue()));
        }
        promotion.setMaxUses(validateMaxUses(request.getMaxUses()));
        if (request.getUsesCount() != null) {
            promotion.setUsesCount(validateUsesCount(request.getUsesCount()));
        }
        if (request.getValidFrom() != null) {
            promotion.setValidFrom(request.getValidFrom());
        }
        promotion.setValidUntil(request.getValidUntil());
        if (request.getIsActive() != null) {
            promotion.setIsActive(request.getIsActive());
        }
        validateDateRange(promotion.getValidFrom(), promotion.getValidUntil());
        promotion.setUpdatedAt(LocalDateTime.now());
        return toResponse(promotionRepository.save(promotion));
    }

    @Transactional
    public void delete(UUID id) {
        Promotion promotion = promotionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Promotion not found"));
        promotionRepository.delete(promotion);
    }

    private PromotionResponse toResponse(Promotion promotion) {
        PromotionResponse response = new PromotionResponse();
        response.setId(promotion.getId());
        response.setCode(promotion.getCode());
        response.setDiscountType(promotion.getDiscountType());
        response.setDiscountValue(promotion.getDiscountValue());
        response.setMaxUses(promotion.getMaxUses());
        response.setUsesCount(promotion.getUsesCount());
        response.setValidFrom(promotion.getValidFrom());
        response.setValidUntil(promotion.getValidUntil());
        response.setIsActive(promotion.getIsActive());
        response.setCreatedAt(promotion.getCreatedAt());
        response.setUpdatedAt(promotion.getUpdatedAt());
        return response;
    }

    private String normalizeCode(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Promotion code is required");
        }
        String code = value.trim();
        if (code.length() > 40) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Promotion code cannot exceed 40 characters");
        }
        return code;
    }

    private BigDecimal normalizeDiscountValue(DiscountType type, BigDecimal value) {
        if (value == null || value.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Discount value must be greater than zero");
        }
        if (type == DiscountType.PERCENTAGE && value.compareTo(new BigDecimal("100")) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Percentage discount cannot exceed 100");
        }
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private Integer validateMaxUses(Integer value) {
        if (value != null && value < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Max uses must be greater than zero");
        }
        return value;
    }

    private Integer validateUsesCount(Integer value) {
        int usesCount = value != null ? value : 0;
        if (usesCount < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Uses count cannot be negative");
        }
        return usesCount;
    }

    private void validateDateRange(LocalDateTime validFrom, LocalDateTime validUntil) {
        if (validFrom != null && validUntil != null && validUntil.isBefore(validFrom)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Valid until cannot be before valid from");
        }
    }
}
