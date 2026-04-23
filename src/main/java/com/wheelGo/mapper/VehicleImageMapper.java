package com.wheelGo.mapper;

import com.wheelGo.model.vehicle_images.VehicleImage;
import com.wheelGo.model.vehicle_images.VehicleImageResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface VehicleImageMapper extends BaseMapper<VehicleImageResponse,VehicleImage>{
}
