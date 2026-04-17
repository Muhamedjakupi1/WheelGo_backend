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


    public static LocationResponse from(Location location) {
        LocationResponse locationResponse = new LocationResponse();
        locationResponse.setId(location.getId());
        locationResponse.setName(location.getName());
        locationResponse.setAddress(location.getAddress());
        locationResponse.setCity(location.getCity());
        locationResponse.setCountry(location.getCountry());
        locationResponse.setLatitude(location.getLatitude());
        locationResponse.setLongitude(location.getLongitude());
        locationResponse.setPhone(location.getPhone());
        locationResponse.setActive(location.isActive());
        return locationResponse;
    }
}
