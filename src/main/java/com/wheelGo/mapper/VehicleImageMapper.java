package com.wheelGo.mapper;

import com.wheelGo.model.vehicleImages.VehicleImage;
import com.wheelGo.model.vehicleImages.VehicleImageResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface VehicleImageMapper extends BaseMapper<VehicleImageResponse,VehicleImage>{
}
