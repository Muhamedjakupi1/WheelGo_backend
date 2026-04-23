package com.wheelGo.mapper;

import com.wheelGo.model.tenantsettings.TenantSettings;
import com.wheelGo.model.tenantsettings.TenantSettingsResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface TenantSettingMapper extends BaseMapper<TenantSettingsResponse,TenantSettings> {
}
