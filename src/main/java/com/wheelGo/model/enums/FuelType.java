package com.wheelGo.model.enums;

@PgEnumType(value = "fuel_type", scope = PgEnumScope.TENANT)
public enum FuelType {
    PETROL,
    DIESEL,
    ELECTRIC,
    HYBRID
}
