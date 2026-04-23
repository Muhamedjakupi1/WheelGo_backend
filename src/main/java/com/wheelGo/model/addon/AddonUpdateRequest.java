package com.wheelGo.model.addon;

import com.wheelGo.model.enums.AddonType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;

@Getter @Setter
public class AddonUpdateRequest {
    @Size(max = 80, message = "Name too long")
    private String name;

    private String description;

    @DecimalMin(value = "0.0", message = "Price must be positive")
    private BigDecimal price;

    private AddonType type;

    private Boolean isActive;
}