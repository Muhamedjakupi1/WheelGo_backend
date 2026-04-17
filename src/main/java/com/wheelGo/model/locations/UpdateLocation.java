package com.wheelGo.model.locations;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;

@Getter
@Setter
public class UpdateLocation {
    private String name;
    private String address;
    @Size(max = 80, message = "City name must not exceed 80 characters")
    private String city;
    @Size(max = 80, message = "Country name must not exceed 80 characters")
    private String country;
    private BigDecimal latitude;
    private BigDecimal longitude;
    @Size(max = 30, message = "Phone number must not exceed 30 characters")
    private String phone;
    private Boolean isActive;
}
