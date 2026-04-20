package com.wheelGo.model.enums;

@PgEnumType(value = "vehicle_status", scope = PgEnumScope.TENANT)
public enum VehicleStatus {
    AVAILABLE,
    RENTED,
    MAINTENANCE,
    INACTIVE
}
