package com.wheelGo.model.addon;

import com.wheelGo.model.enums.AddonType;
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
    private Integer quantity;
    private AddonType type;
    private Boolean isActive;
    private Boolean inventoryManaged;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
