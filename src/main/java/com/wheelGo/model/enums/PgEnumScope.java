package com.wheelGo.model.enums;

public enum PgEnumScope {
    PUBLIC("public"),
    TENANT(null);

    private final String schemaName;

    PgEnumScope(String schemaName) {
        this.schemaName = schemaName;
    }

    public String schemaName() {
        return schemaName;
    }
}
