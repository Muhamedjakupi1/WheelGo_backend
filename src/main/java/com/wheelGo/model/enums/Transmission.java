package com.wheelGo.model.enums;

@PgEnumType(value = "transmission_type", scope = PgEnumScope.TENANT)
public enum Transmission {
    MANUAL,
    AUTOMATIC
}
