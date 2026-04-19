package com.wheelGo.schema;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.springframework.stereotype.Service;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

@Service
@Slf4j
@AllArgsConstructor
public class TenantSchemaService {

    private final DataSource dataSource;

    public void createSchemaForTenant(String schemaName) {
        validateSchemaName(schemaName);
        createSchema(schemaName);
        runMigrations(schemaName);
    }

    private void validateSchemaName(String schemaName) {
        if (!schemaName.matches("^[a-z][a-z0-9_]{0,62}$")) {
            throw new IllegalArgumentException(
                    "Schema name i pavlefshëm: " + schemaName
            );
        }
    }

    private void createSchema(String schemaName) {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE SCHEMA IF NOT EXISTS \"" + schemaName + "\"");
            log.info("Schema '{}' u krijua.", schemaName);
        } catch (SQLException e) {
            throw new RuntimeException(
                    "Gabim duke krijuar schemën: " + schemaName, e
            );
        }
    }

    private void runMigrations(String schemaName) {
        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .schemas(schemaName)
                .defaultSchema(schemaName)
                .locations("classpath:db/tenant")
                .table("flyway_schema_history")
                .baselineOnMigrate(true)
                .load();

        flyway.migrate();
        log.info("Migrations u ekzekutuan për schemën    '{}'.", schemaName);
    }
    public void dropSchemaForTenant(String schemaName) {
        validateSchemaName(schemaName);
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("DROP SCHEMA IF EXISTS \"" + schemaName + "\" CASCADE");
            log.info("Schema '{}' u fshi.", schemaName);
        } catch (SQLException e) {
            throw new RuntimeException(
                    "Gabim duke fshirë schemën: " + schemaName, e
            );
        }
    }
}