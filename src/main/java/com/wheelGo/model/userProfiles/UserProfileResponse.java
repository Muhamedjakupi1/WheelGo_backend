package com.wheelGo.model.userProfiles;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;
import java.util.UUID;

@Getter @Setter
public class UserProfileResponse {

    private UUID      id;
    private UUID      userId;
    private String    firstName;
    private String    lastName;
    private String    phone;
    private String    avatarUrl;
    private LocalDate dateOfBirth;
    private String    address;
    private String    city;
    private String    country;

    public static UserProfileResponse from(UserProfile p) {
        UserProfileResponse r = new UserProfileResponse();
        r.setId(p.getId());
        r.setUserId(p.getUser().getId());
        r.setFirstName(p.getFirstName());
        r.setLastName(p.getLastName());
        r.setPhone(p.getPhone());
        r.setAvatarUrl(p.getAvatarUrl());
        r.setDateOfBirth(p.getDateOfBirth());
        r.setAddress(p.getAddress());
        r.setCity(p.getCity());
        r.setCountry(p.getCountry());
        return r;
    }
}