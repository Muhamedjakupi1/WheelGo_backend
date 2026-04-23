package com.wheelGo.mapper;

import com.wheelGo.model.vehicle_categories.VehicleCategory;
import com.wheelGo.model.vehicle_categories.VehicleCategoryResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface VehicleCategoryMapper extends BaseMapper<VehicleCategoryResponse, VehicleCategory> {
}
