package com.wheelGo.model.tenantSettings;

import lombok.Getter;
import lombok.Setter;
import java.util.UUID;

@Getter @Setter
public class TenantSettingsResponse {

    private UUID   id;
    private String currency;
    private String timezone;
    private String logoUrl;
    private String themeColor;
}