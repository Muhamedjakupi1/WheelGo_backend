package com.wheelGo.model.locations;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter @Setter
public class LocationResponse {
    private UUID id;
    private String name;
    private String address;
    private String city;
    private String country;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String phone;
    private boolean isActive;
}
