package com.wheelGo.model.addon;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
public class AddonResponse {
    private UUID id;
    private String name;
    private String description;
    private BigDecimal price;
    private String type;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static AddonResponse from(Addon addon) {
        AddonResponse res = new AddonResponse();
        res.setId(addon.getId());
        res.setName(addon.getName());
        res.setDescription(addon.getDescription());
        res.setPrice(addon.getPrice());
        res.setType(addon.getType());
        res.setIsActive(addon.getIsActive());
        res.setCreatedAt(addon.getCreatedAt());
        res.setUpdatedAt(addon.getUpdatedAt());
        return res;
    }
}