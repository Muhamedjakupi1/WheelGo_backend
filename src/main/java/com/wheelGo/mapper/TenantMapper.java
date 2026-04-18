package com.wheelGo.mapper;

import com.wheelGo.model.tenant.Tenant;
import com.wheelGo.model.tenant.TenantResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface TenantMapper extends BaseMapper<TenantResponse, Tenant> {
}
