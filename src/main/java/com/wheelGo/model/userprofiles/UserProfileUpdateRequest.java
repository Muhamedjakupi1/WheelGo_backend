package com.wheelGo.model.userprofiles;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;

@Getter @Setter
public class UserProfileUpdateRequest {
    private String    firstName;
    private String    lastName;
    private String    phone;
    private String    avatarUrl;
    private LocalDate dateOfBirth;
    private String    address;
    private String    city;
    private String    country;
}