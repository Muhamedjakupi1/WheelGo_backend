package com.wheelGo.model.enums;

@PgEnumType(value = "user_role", scope = PgEnumScope.PUBLIC)
public enum Role {
    SUPER_ADMIN,
    ADMIN,
    USER
}
