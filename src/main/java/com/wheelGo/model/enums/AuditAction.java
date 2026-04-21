package com.wheelGo.model.enums;

@PgEnumType(value = "audit_action", scope = PgEnumScope.TENANT)
public enum AuditAction {
    CREATE,
    UPDATE,
    DELETE,
    LOGIN,
    LOGOUT
}
