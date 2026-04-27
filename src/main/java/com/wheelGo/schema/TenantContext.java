package com.wheelGo.schema;

import java.util.function.Supplier;

public final class TenantContext {

    private static final ThreadLocal<String> CURRENT_SCHEMA = new ThreadLocal<>();

    private TenantContext() {
    }

    public static void setCurrentSchema(String schemaName) {
        CURRENT_SCHEMA.set(schemaName);
    }

    public static String getCurrentSchema() {
        return CURRENT_SCHEMA.get();
    }

    public static void clear() {
        CURRENT_SCHEMA.remove();
    }

    public static void runWithSchema(String schemaName, Runnable action) {
        String previousSchema = getCurrentSchema();
        try {
            setCurrentSchema(schemaName);
            action.run();
        } finally {
            restore(previousSchema);
        }
    }

    public static <T> T callWithSchema(String schemaName, Supplier<T> supplier) {
        String previousSchema = getCurrentSchema();
        try {
            setCurrentSchema(schemaName);
            return supplier.get();
        } finally {
            restore(previousSchema);
        }
    }

    private static void restore(String previousSchema) {
        if (previousSchema == null || previousSchema.isBlank()) {
            clear();
            return;
        }

        setCurrentSchema(previousSchema);
    }
}
