package com.wheelGo.model.enums;

@PgEnumType(value = "chat_role", scope = PgEnumScope.TENANT)
public enum ChatRole {
    USER,
    ASSISTANT,
    SYSTEM
}
