package com.wheelGo.model.tenant;

import com.wheelGo.model.enums.Plan;
import com.wheelGo.model.tenant_settings.TenantSettingsResponse;
import lombok.Getter;
import lombok.Setter;
import java.util.UUID;
import java.time.LocalDateTime;

@Getter @Setter
public class TenantResponse {

    private UUID id;
    private String name;
    private String slug;
    private String schemaName;
    private Plan plan;
    private boolean isActive;
    private TenantSettingsResponse settings;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
