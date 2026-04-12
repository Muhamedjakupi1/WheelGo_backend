package com.wheelGo.model.tenantSettings;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class CreateTenantSettingsRequest {

    @NotBlank(message = "Currency cannot be empty")
    private String currency;

    @NotBlank(message = "Timezone cannot be empty")
    private String timezone;

    private String logoUrl;

    private String themeColor;
}