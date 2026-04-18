package com.wheelGo.mapper;

import com.wheelGo.model.driverLicenses.DriverLicense;
import com.wheelGo.model.driverLicenses.DriverLicenseResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface DriverLicenseMapper extends BaseMapper<DriverLicenseResponse, DriverLicense> {
}
