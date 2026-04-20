package com.wheelGo.model.enums;

@PgEnumType(value = "discount_type", scope = PgEnumScope.TENANT)
public enum DiscountType {
    PERCENTAGE,
    FIXED
}
