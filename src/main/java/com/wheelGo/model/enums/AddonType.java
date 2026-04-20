package com.wheelGo.model.enums;

@PgEnumType(value = "addon_type", scope = PgEnumScope.TENANT)
public enum AddonType {
    DAILY,
    ONE_TIME
}
