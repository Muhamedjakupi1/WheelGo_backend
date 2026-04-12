package com.wheelGo.model.tenant;

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
    private String plan;
    private boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static TenantResponse from(Tenant t) {
        TenantResponse r = new TenantResponse();
        r.setId(t.getId());
        r.setName(t.getName());
        r.setSlug(t.getSlug());
        r.setSchemaName(t.getSchemaName());
        r.setPlan(t.getPlan());
        r.setActive(t.isActive());
        r.setCreatedAt(t.getCreatedAt());
        r.setUpdatedAt(t.getUpdatedAt());

        return r;
    }
}