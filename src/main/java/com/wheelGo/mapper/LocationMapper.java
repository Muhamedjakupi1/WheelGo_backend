package com.wheelGo.mapper;

import com.wheelGo.model.locations.Location;
import com.wheelGo.model.locations.LocationResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface LocationMapper extends BaseMapper<LocationResponse,Location> {
}
