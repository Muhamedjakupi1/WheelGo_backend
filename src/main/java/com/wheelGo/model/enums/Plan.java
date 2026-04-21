package com.wheelGo.model.enums;

@PgEnumType(value = "tenant_plan", scope = PgEnumScope.PUBLIC)
public enum Plan {
    FREE,
    BASIC,
    PREMIUM,
    ENTERPRISE
}
