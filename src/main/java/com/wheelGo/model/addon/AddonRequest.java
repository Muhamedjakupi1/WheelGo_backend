package com.wheelGo.model.addon;

import com.wheelGo.model.enums.AddonType;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class AddonRequest {
    private String name;
    private String description;
    private BigDecimal price;
    private AddonType type;
    private Boolean isActive;
}