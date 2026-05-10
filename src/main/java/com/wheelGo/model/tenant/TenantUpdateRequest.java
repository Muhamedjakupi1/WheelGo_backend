package com.wheelGo.model.tenant;

import com.wheelGo.model.enums.Plan;
import com.wheelGo.model.tenant_settings.TenantSettingsUpdateRequest;
import jakarta.validation.Valid;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class TenantUpdateRequest {
    private String  name;
    private Plan plan;
    private Boolean isActive;

    @Valid
    private TenantSettingsUpdateRequest settings;
}
