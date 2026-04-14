package com.wheelGo.model.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class UpdateUserRequest {

    @Email(message = "Email nuk është valid")
    private String email;

    @Size(min = 8, message = "Fjalëkalimi duhet të ketë minimum 8 karaktere")
    private String password;

    private String  role;
    private Boolean isActive;
}