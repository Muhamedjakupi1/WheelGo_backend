package com.wheelGo.model.tenantsettings;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class TenantSettingsRequest {

    @NotBlank(message = "Currency cannot be empty")
    private String currency;

    @NotBlank(message = "Timezone cannot be empty")
    private String timezone;

    private String logoUrl;

    private String themeColor;
}