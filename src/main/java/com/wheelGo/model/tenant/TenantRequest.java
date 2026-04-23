package com.wheelGo.model.tenant;

import com.wheelGo.model.enums.Plan;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class TenantRequest {

    @NotBlank(message = "Name cannot be empty")
    private String name;

    @NotBlank
    @Pattern(
            regexp = "^[a-z0-9-]{2,50}$",
            message = "Slug: only lowercase letters, numbers and hyphen are allowed"
    )
    private String slug;

    private Plan plan = Plan.FREE;

    private String adminEmail;
    private String adminPassword;
}