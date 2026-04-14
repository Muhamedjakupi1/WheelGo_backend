package com.wheelGo.model.user;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class CreateUserRequest {

    @NotBlank(message = "Email cannot be empty")
    @Email(message = "Email is not valid")
    private String email;

    @NotBlank(message = "Password cannot be empty")
    @Size(min = 8, message = "Password must have at least 8 characters")
    private String password;

    private String role = "CUSTOMER";
}