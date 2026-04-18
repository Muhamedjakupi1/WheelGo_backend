package com.wheelGo.mapper;

import com.wheelGo.model.vehicleCategories.VehicleCategory;
import com.wheelGo.model.vehicleCategories.VehicleCategoryResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface VehicleCategoryMapper extends BaseMapper<VehicleCategoryResponse, VehicleCategory> {
}
