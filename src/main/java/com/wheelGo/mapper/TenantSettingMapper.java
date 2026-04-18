package com.wheelGo.mapper;

import com.wheelGo.model.tenantSettings.TenantSettings;
import com.wheelGo.model.tenantSettings.TenantSettingsResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface TenantSettingMapper extends BaseMapper<TenantSettingsResponse,TenantSettings> {
}
