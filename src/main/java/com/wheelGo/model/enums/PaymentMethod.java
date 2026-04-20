package com.wheelGo.model.enums;

@PgEnumType(value = "payment_method", scope = PgEnumScope.TENANT)
public enum PaymentMethod {
    CARD,
    CASH
}
