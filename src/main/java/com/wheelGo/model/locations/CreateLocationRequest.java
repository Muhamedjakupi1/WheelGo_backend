package com.wheelGo.model.locations;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter @Setter
public class CreateLocationRequest {

    @NotBlank(message = "Name of the location is required")
    @Size(max = 100)
    private String name;

    @NotBlank(message = "Addres is required")
    private String addres;

    @NotBlank(message = "City is required")
    private String city;

    private BigDecimal latitude;
    private BigDecimal longitude;
    private String phone;
}
