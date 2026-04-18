package com.wheelGo.mapper;

import com.wheelGo.model.vehicles.Vehicle;
import com.wheelGo.model.vehicles.VehicleResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface VehicleMapper extends BaseMapper<VehicleResponse, Vehicle> {
}
