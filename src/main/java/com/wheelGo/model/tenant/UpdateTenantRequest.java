package com.wheelGo.model.tenant;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class UpdateTenantRequest {
    private String  name;
    private String  plan;
    private Boolean isActive;
}