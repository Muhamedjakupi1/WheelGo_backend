package com.wheelGo.security;

public final class ReservedTenantSlugs {

    public static final String SUPER_ADMIN_TENANT = "super-admin-tenant";

    private ReservedTenantSlugs() {
    }

    public static boolean isReserved(String tenantSlug) {
        return SUPER_ADMIN_TENANT.equalsIgnoreCase(tenantSlug);
    }
}
