package com.wheelGo.mapper;

import com.wheelGo.model.promotions.Promotion;
import com.wheelGo.model.promotions.PromotionResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PromotionMapper extends BaseMapper<PromotionResponse, Promotion> {
}
