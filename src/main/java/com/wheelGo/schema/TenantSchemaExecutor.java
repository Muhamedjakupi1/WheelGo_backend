package com.wheelGo.schema;

import com.wheelGo.model.tenant.Tenant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.function.Supplier;

@Service
@RequiredArgsConstructor
public class TenantSchemaExecutor {

    private final TenantTransactionalExecutor tenantTransactionalExecutor;

    public void runInSchema(String schemaName, Runnable action) {
        TenantContext.runWithSchema(schemaName,
                () -> tenantTransactionalExecutor.runInTransaction(action));
    }

    public <T> T callInSchema(String schemaName, Supplier<T> supplier) {
        return TenantContext.callWithSchema(schemaName,
                () -> tenantTransactionalExecutor.callInTransaction(supplier));
    }

    public void runForTenant(Tenant tenant, Runnable action) {
        runInSchema(requireSchemaName(tenant), action);
    }

    public <T> T callForTenant(Tenant tenant, Supplier<T> supplier) {
        return callInSchema(requireSchemaName(tenant), supplier);
    }

    private String requireSchemaName(Tenant tenant) {
        if (tenant == null || tenant.getSchemaName() == null || tenant.getSchemaName().isBlank()) {
            throw new IllegalArgumentException("Tenant schema name is required");
        }

        return tenant.getSchemaName();
    }
}
