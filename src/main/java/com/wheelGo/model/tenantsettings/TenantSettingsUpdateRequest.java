package com.wheelGo.model.tenantsettings;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class TenantSettingsUpdateRequest {
    private String currency;
    private String timezone;
    private String logoUrl;
    private String themeColor;
}