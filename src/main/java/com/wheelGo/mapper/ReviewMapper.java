package com.wheelGo.mapper;

import com.wheelGo.model.reviews.Review;
import com.wheelGo.model.reviews.ReviewResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ReviewMapper extends BaseMapper<ReviewResponse, Review> {
}
