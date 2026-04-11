package com.wheelGo.model.tenant;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class CreateTenantRequest {

    @NotBlank(message = "Emri nuk mund të jetë bosh")
    private String name;

    @NotBlank
    @Pattern(
            regexp = "^[a-z0-9-]{2,50}$",
            message = "Slug: vetëm shkronja të vogla, numra dhe vizë"
    )
    private String slug;

    private String plan = "FREE";
}