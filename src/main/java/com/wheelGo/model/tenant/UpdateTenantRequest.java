package com.wheelGo.model.tenant;

import com.wheelGo.model.enums.Plan;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class UpdateTenantRequest {
    private String  name;
    private Plan plan;
    private Boolean isActive;
}