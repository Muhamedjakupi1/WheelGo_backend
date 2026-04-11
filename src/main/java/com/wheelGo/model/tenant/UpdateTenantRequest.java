package com.wheelGo.model.tenant;

import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class UpdateTenantRequest {

    private String name;

    @Pattern(
            regexp = "^[a-z0-9-]{2,50}$",
            message = "Slug: vetëm shkronja të vogla, numra dhe vizë"
    )
    private String slug;

    private String  plan;
    private Boolean isActive;
}