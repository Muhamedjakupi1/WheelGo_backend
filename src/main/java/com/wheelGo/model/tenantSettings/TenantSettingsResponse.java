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

    public static TenantSettingsResponse from(TenantSettings s) {
        TenantSettingsResponse r = new TenantSettingsResponse();
        r.setId(s.getId());
        r.setCurrency(s.getCurrency());
        r.setTimezone(s.getTimezone());
        r.setLogoUrl(s.getLogoUrl());
        r.setThemeColor(s.getThemeColor());
        return r;
    }
}