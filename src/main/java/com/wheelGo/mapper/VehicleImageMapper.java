package com.wheelGo.mapper;

import com.wheelGo.model.vehicleimages.VehicleImage;
import com.wheelGo.model.vehicleimages.VehicleImageResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface VehicleImageMapper extends BaseMapper<VehicleImageResponse,VehicleImage>{
}
