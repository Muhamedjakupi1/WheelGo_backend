package com.wheelGo.schema;

import com.wheelGo.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Order(1)
@RequiredArgsConstructor
@Slf4j
public class TenantSchemaStartupMigrator implements ApplicationRunner {

    private final TenantRepository tenantRepository;
    private final TenantSchemaService tenantSchemaService;

    @Value("${app.tenants.migrate-on-startup:true}")
    private boolean migrateOnStartup;

    @Override
    public void run(ApplicationArguments args) {
        if (!migrateOnStartup) {
            log.info("Tenant schema startup migration is disabled.");
            return;
        }

        List<String> schemaNames = tenantRepository.findAll().stream()
                .map(tenant -> tenant.getSchemaName())
                .filter(schemaName -> schemaName != null && !schemaName.isBlank())
                .distinct()
                .sorted()
                .toList();

        if (schemaNames.isEmpty()) {
            log.info("No tenant schemas found for startup migration.");
            return;
        }

        log.info("Starting tenant schema migration for {} schema(s).", schemaNames.size());
        tenantSchemaService.migrateExistingSchemas(schemaNames);
        log.info("Tenant schema migration completed for {} schema(s).", schemaNames.size());
    }
}
