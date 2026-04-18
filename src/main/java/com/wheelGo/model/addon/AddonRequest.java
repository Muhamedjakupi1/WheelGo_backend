package com.wheelGo.model.addon;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class AddonRequest {
    private String name;
    private String description;
    private BigDecimal price;
    private String type;
    private Boolean isActive;
}