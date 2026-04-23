package com.wheelGo.model.user;

import com.wheelGo.model.enums.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class UserUpdateRequest {

    @Email(message = "Email nuk është valid")
    private String email;

    @Size(min = 8, message = "Fjalëkalimi duhet të ketë minimum 8 karaktere")
    private String password;

    private Role    role;
    private Boolean isActive;
}