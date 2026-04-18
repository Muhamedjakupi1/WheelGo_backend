package com.wheelGo.mapper;

import com.wheelGo.model.addon.Addon;
import com.wheelGo.model.addon.AddonResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AddonMapper {
    AddonResponse toResponse(Addon addon);
}
