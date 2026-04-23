package com.wheelGo.mapper;

import com.wheelGo.model.tenant_settings.TenantSettings;
import com.wheelGo.model.tenant_settings.TenantSettingsResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface TenantSettingMapper extends BaseMapper<TenantSettingsResponse,TenantSettings> {
}
