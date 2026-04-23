package com.wheelGo.mapper;

import com.wheelGo.model.vehiclecategories.VehicleCategory;
import com.wheelGo.model.vehiclecategories.VehicleCategoryResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface VehicleCategoryMapper extends BaseMapper<VehicleCategoryResponse, VehicleCategory> {
}
