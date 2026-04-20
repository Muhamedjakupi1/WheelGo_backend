package com.wheelGo.model.enums;

@PgEnumType(value = "booking_status", scope = PgEnumScope.TENANT)
public enum BookingStatus {
    PENDING,
    CONFIRMED,
    ACTIVE,
    COMPLETED,
    CANCELLED
}
