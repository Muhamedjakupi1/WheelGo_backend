package com.wheelGo.model.reviews;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class ReviewUpdateRequest {

    @Min(value = 1, message = "Rating must be at leat 1")
    @Max(value = 5, message = "Rating cannot be more than 5")
    private Integer rating;

    private String comment;

}
