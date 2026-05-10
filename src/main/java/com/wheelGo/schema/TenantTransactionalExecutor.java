package com.wheelGo.schema;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.function.Supplier;

@Service
public class TenantTransactionalExecutor {

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void runInTransaction(Runnable action) {
        applyLocalSearchPath();
        action.run();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public <T> T callInTransaction(Supplier<T> supplier) {
        applyLocalSearchPath();
        return supplier.get();
    }

    private void applyLocalSearchPath() {
        String schemaName = TenantContext.getCurrentSchema();
        if (schemaName == null || !schemaName.matches("^[a-z][a-z0-9_]{0,62}$")) {
            throw new IllegalArgumentException("Invalid schema name: " + schemaName);
        }

        entityManager.createNativeQuery("SET LOCAL search_path TO \"" + schemaName + "\", public")
                .executeUpdate();
    }
}
