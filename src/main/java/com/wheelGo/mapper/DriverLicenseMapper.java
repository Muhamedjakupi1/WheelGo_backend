package com.wheelGo.mapper;

import com.wheelGo.model.driverlicenses.DriverLicense;
import com.wheelGo.model.driverlicenses.DriverLicenseResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface DriverLicenseMapper extends BaseMapper<DriverLicenseResponse, DriverLicense> {
}
