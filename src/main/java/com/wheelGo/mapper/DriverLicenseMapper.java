package com.wheelGo.mapper;

import com.wheelGo.model.driver_licenses.DriverLicense;
import com.wheelGo.model.driver_licenses.DriverLicenseResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface DriverLicenseMapper extends BaseMapper<DriverLicenseResponse, DriverLicense> {
}
