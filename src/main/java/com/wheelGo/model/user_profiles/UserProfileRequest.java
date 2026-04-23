package com.wheelGo.model.user_profiles;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;

@Getter @Setter
public class UserProfileRequest {

    @NotBlank(message = "First name cannot be empty")
    private String firstName;

    @NotBlank(message = "Last name cannot be empty")
    private String lastName;

    private String    phone;
    private String    avatarUrl;
    private LocalDate dateOfBirth;
    private String    address;
    private String    city;
    private String    country;
}