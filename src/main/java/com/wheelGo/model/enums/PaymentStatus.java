package com.wheelGo.model.enums;

@PgEnumType(value = "payment_status", scope = PgEnumScope.TENANT)
public enum PaymentStatus {
    PENDING,
    PAID,
    FAILED,
    REFUNDED
}
