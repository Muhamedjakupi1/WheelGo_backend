package com.wheelGo.tools;

import org.flywaydb.core.Flyway;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public final class MigrationApplier {

    private static final Path APP_PROPERTIES = Paths.get("src", "main", "resources", "application.properties");

    private MigrationApplier() {
    }

    public static void main(String[] args) throws Exception {
        Properties properties = loadApplicationProperties();
        String url = requireProperty(properties, "spring.datasource.url");
        String username = requireProperty(properties, "spring.datasource.username");
        String password = requireProperty(properties, "spring.datasource.password");

        migratePublic(url, username, password);
        migrateTenants(url, username, password);
    }

    private static void migratePublic(String url, String username, String password) {
        Flyway flyway = Flyway.configure()
                .dataSource(url, username, password)
                .locations("classpath:db/migration/public")
                .schemas("public")
                .defaultSchema("public")
                .load();

        flyway.migrate();
        System.out.println("Applied public migrations.");
    }

    private static void migrateTenants(String url, String username, String password) throws SQLException {
        try (Connection connection = DriverManager.getConnection(url, username, password)) {
            List<String> schemaNames = loadTenantSchemas(connection);
            if (schemaNames.isEmpty()) {
                System.out.println("No tenant schemas found. Skipping tenant migrations.");
                return;
            }

            for (String schemaName : schemaNames) {
                Flyway flyway = Flyway.configure()
                        .dataSource(url, username, password)
                        .locations("classpath:db/migration/tenant")
                        .schemas(schemaName)
                        .defaultSchema(schemaName)
                        .table("flyway_schema_history")
                        .baselineOnMigrate(true)
                        .load();

                flyway.migrate();
                System.out.println("Applied tenant migrations for schema '" + schemaName + "'.");
            }
        }
    }

    private static List<String> loadTenantSchemas(Connection connection) throws SQLException {
        List<String> schemas = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(
                "select distinct schema_name from public.tenants where schema_name is not null and schema_name <> '' order by schema_name"
        )) {
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    schemas.add(rs.getString(1));
                }
            }
        }
        return schemas;
    }

    private static Properties loadApplicationProperties() throws IOException {
        Properties properties = new Properties();
        try (var reader = Files.newBufferedReader(APP_PROPERTIES, StandardCharsets.UTF_8)) {
            properties.load(reader);
        }
        return properties;
    }

    private static String requireProperty(Properties properties, String key) {
        String value = properties.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing property: " + key);
        }
        return value.trim();
    }
}
