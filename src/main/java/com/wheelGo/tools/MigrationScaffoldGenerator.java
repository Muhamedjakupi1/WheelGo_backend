package com.wheelGo.tools;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class MigrationScaffoldGenerator {

    private static final Path CLASSES_ROOT = Paths.get("target", "classes", "com", "wheelGo", "model");
    private static final Path APP_PROPERTIES = Paths.get("src", "main", "resources", "application.properties");
    private static final Path PUBLIC_MIGRATIONS = Paths.get("src", "main", "resources", "db", "migration", "public");
    private static final Path TENANT_MIGRATIONS = Paths.get("src", "main", "resources", "db", "migration", "tenant");
    private static final Pattern VERSION_PATTERN = Pattern.compile("^V(\\d+)__.*\\.sql$");

    private MigrationScaffoldGenerator() {
    }

    public static void main(String[] args) throws Exception {
        Arguments parsed = Arguments.parse(args);
        Properties appProps = loadApplicationProperties();

        List<EntityModel> entities = discoverEntities();
        if (entities.isEmpty()) {
            throw new IllegalStateException("No @Entity classes found under " + CLASSES_ROOT);
        }

        try (Connection connection = DriverManager.getConnection(
                requireProperty(appProps, "spring.datasource.url"),
                requireProperty(appProps, "spring.datasource.username"),
                requireProperty(appProps, "spring.datasource.password")
        )) {
            List<EntityModel> publicEntities = entities.stream()
                    .filter(entity -> entity.scope == Scope.PUBLIC)
                    .sorted(Comparator.comparing(entity -> entity.tableName))
                    .toList();

            List<String> publicStatements = generateStatements(connection, "public", publicEntities, true);
            writeMigrationIfNeeded(PUBLIC_MIGRATIONS, parsed.migrationName, publicStatements, "public schema");

            String tenantSchema = resolveTenantSchema(connection, parsed.tenantSchemaOverride);
            if (tenantSchema == null) {
                System.out.println("No tenant schema found. Skipping tenant schema update check.");
            } else {
                List<EntityModel> tenantEntities = entities.stream()
                        .filter(entity -> entity.scope == Scope.TENANT)
                        .sorted(Comparator.comparing(entity -> entity.tableName))
                        .toList();

                List<String> tenantStatements = generateStatements(connection, tenantSchema, tenantEntities, false);
                writeMigrationIfNeeded(TENANT_MIGRATIONS, parsed.migrationName, tenantStatements, "tenant schemas");
            }
        }
    }

    private static Properties loadApplicationProperties() throws IOException {
        if (!Files.exists(APP_PROPERTIES)) {
            throw new IllegalStateException("Missing application properties at " + APP_PROPERTIES);
        }

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

    private static List<EntityModel> discoverEntities() throws IOException, ClassNotFoundException {
        if (!Files.exists(CLASSES_ROOT)) {
            throw new IllegalStateException("Compiled classes not found. Run Maven compile first.");
        }

        try (Stream<Path> stream = Files.walk(CLASSES_ROOT)) {
            List<Path> classFiles = stream
                    .filter(path -> path.toString().endsWith(".class"))
                    .filter(path -> !path.getFileName().toString().contains("$"))
                    .toList();

            List<EntityModel> entities = new ArrayList<>();
            for (Path classFile : classFiles) {
                String className = toClassName(classFile);
                Class<?> candidate = Class.forName(className);
                if (candidate.getAnnotation(Entity.class) != null) {
                    entities.add(inspectEntity(candidate));
                }
            }
            return entities;
        }
    }

    private static String toClassName(Path classFile) {
        Path relative = Paths.get("target", "classes").relativize(classFile);
        String normalized = relative.toString().replace('\\', '.').replace('/', '.');
        return normalized.substring(0, normalized.length() - ".class".length());
    }

    private static EntityModel inspectEntity(Class<?> entityClass) {
        Table table = entityClass.getAnnotation(Table.class);
        String tableName = table != null && !table.name().isBlank()
                ? table.name()
                : camelToSnake(entityClass.getSimpleName());
        String schemaName = table != null && !table.schema().isBlank()
                ? table.schema()
                : null;

        Scope scope = "public".equalsIgnoreCase(schemaName) ? Scope.PUBLIC : Scope.TENANT;
        List<ColumnModel> columns = new ArrayList<>();

        for (Field field : entityClass.getDeclaredFields()) {
            if (shouldSkip(field)) {
                continue;
            }

            ColumnModel model = inspectField(field);
            if (model != null) {
                columns.add(model);
            }
        }

        return new EntityModel(entityClass, tableName, schemaName, scope, columns);
    }

    private static boolean shouldSkip(Field field) {
        int modifiers = field.getModifiers();
        return Modifier.isStatic(modifiers)
                || Modifier.isTransient(modifiers)
                || field.getAnnotation(jakarta.persistence.Transient.class) != null
                || field.getAnnotation(OneToMany.class) != null
                || field.getAnnotation(ManyToMany.class) != null;
    }

    private static ColumnModel inspectField(Field field) {
        boolean relation = field.getAnnotation(ManyToOne.class) != null || field.getAnnotation(OneToOne.class) != null;
        if (relation) {
            JoinColumn joinColumn = field.getAnnotation(JoinColumn.class);
            String columnName = joinColumn != null && !joinColumn.name().isBlank()
                    ? joinColumn.name()
                    : camelToSnake(field.getName()) + "_id";
            boolean nullable = joinColumn == null || joinColumn.nullable();
            boolean unique = joinColumn != null && joinColumn.unique();
            ReferenceModel reference = buildReference(field.getType());

            return new ColumnModel(
                    columnName,
                    sqlTypeForId(reference.idType()),
                    nullable,
                    unique,
                    field.getAnnotation(Id.class) != null,
                    reference,
                    false
            );
        }

        if (field.getAnnotation(OneToMany.class) != null || field.getAnnotation(ManyToMany.class) != null) {
            return null;
        }

        Column column = field.getAnnotation(Column.class);
        String columnName = column != null && !column.name().isBlank()
                ? column.name()
                : camelToSnake(field.getName());
        boolean nullable = column == null || column.nullable();
        boolean unique = column != null && column.unique();
        boolean id = field.getAnnotation(Id.class) != null;
        String sqlType = resolveSqlType(field, column);

        return new ColumnModel(
                columnName,
                sqlType,
                id || nullable,
                unique,
                id,
                null,
                true
        );
    }

    private static ReferenceModel buildReference(Class<?> targetEntity) {
        Table table = targetEntity.getAnnotation(Table.class);
        if (table == null) {
            throw new IllegalStateException("Relation target is missing @Table: " + targetEntity.getName());
        }

        Field idField = findIdField(targetEntity)
                .orElseThrow(() -> new IllegalStateException("Relation target is missing @Id: " + targetEntity.getName()));
        String schema = table.schema().isBlank() ? null : table.schema();
        return new ReferenceModel(
                table.name(),
                schema,
                idField.getAnnotation(Column.class) != null && !idField.getAnnotation(Column.class).name().isBlank()
                        ? idField.getAnnotation(Column.class).name()
                        : camelToSnake(idField.getName()),
                idField.getType()
        );
    }

    private static Optional<Field> findIdField(Class<?> type) {
        for (Field field : type.getDeclaredFields()) {
            if (field.getAnnotation(Id.class) != null) {
                return Optional.of(field);
            }
        }
        return Optional.empty();
    }

    private static String resolveSqlType(Field field, Column column) {
        if (column != null && !column.columnDefinition().isBlank()) {
            return column.columnDefinition();
        }

        Class<?> type = field.getType();
        if (type == String.class) {
            if (column != null && column.length() != 255) {
                return "VARCHAR(" + column.length() + ")";
            }
            return "VARCHAR(255)";
        }
        if (type == UUID.class) {
            return "UUID";
        }
        if (type == Integer.class || type == int.class) {
            return "INTEGER";
        }
        if (type == Long.class || type == long.class) {
            return "BIGINT";
        }
        if (type == Short.class || type == short.class) {
            return "SMALLINT";
        }
        if (type == Boolean.class || type == boolean.class) {
            return "BOOLEAN";
        }
        if (type == BigDecimal.class) {
            if (column != null && column.precision() > 0) {
                int scale = Math.max(column.scale(), 0);
                return "NUMERIC(" + column.precision() + "," + scale + ")";
            }
            return "NUMERIC(38,2)";
        }
        if (type == LocalDateTime.class) {
            return "TIMESTAMP";
        }
        if (type == LocalDate.class) {
            return "DATE";
        }
        if (type == OffsetDateTime.class || type == ZonedDateTime.class) {
            return "TIMESTAMP WITH TIME ZONE";
        }
        if (type.isEnum()) {
            return "VARCHAR(255)";
        }

        throw new IllegalStateException("Unsupported field type for migration generation: " + field);
    }

    private static String sqlTypeForId(Class<?> idType) {
        if (idType == UUID.class) {
            return "UUID";
        }
        if (idType == Long.class || idType == long.class) {
            return "BIGINT";
        }
        if (idType == Integer.class || idType == int.class) {
            return "INTEGER";
        }
        throw new IllegalStateException("Unsupported relation id type: " + idType.getName());
    }

    private static List<String> generateStatements(
            Connection connection,
            String schemaName,
            List<EntityModel> entities,
            boolean qualifyWithSchema
    ) throws SQLException {
        List<String> statements = new ArrayList<>();
        DatabaseMetaData metaData = connection.getMetaData();

        for (EntityModel entity : entities) {
            boolean tableExists = tableExists(metaData, schemaName, entity.tableName);
            if (!tableExists) {
                statements.add(buildCreateTable(entity, qualifyWithSchema));
                for (ColumnModel column : entity.foreignKeys()) {
                    statements.add(buildForeignKey(entity, column, qualifyWithSchema));
                }
                continue;
            }

            Set<String> existingColumns = existingColumns(metaData, schemaName, entity.tableName);
            Set<String> existingUniqueColumns = existingUniqueColumns(metaData, schemaName, entity.tableName);
            Set<String> existingFkColumns = existingForeignKeyColumns(metaData, schemaName, entity.tableName);
            Set<String> entityColumns = entity.columns.stream()
                    .map(column -> column.name)
                    .collect(Collectors.toCollection(LinkedHashSet::new));

            for (ColumnModel column : entity.columns) {
                if (!existingColumns.contains(column.name)) {
                    statements.add(buildAddColumn(entity, column, qualifyWithSchema));
                }

                if (column.unique && !column.id && !existingUniqueColumns.contains(column.name)) {
                    statements.add(buildUniqueIndex(entity, column, qualifyWithSchema));
                }

                if (column.reference != null && !existingFkColumns.contains(column.name)) {
                    statements.add(buildForeignKey(entity, column, qualifyWithSchema));
                }
            }

            for (String existingColumn : existingColumns) {
                if (!entityColumns.contains(existingColumn)) {
                    statements.add(buildDropColumn(entity, existingColumn, qualifyWithSchema));
                }
            }
        }

        return statements.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    private static boolean tableExists(DatabaseMetaData metaData, String schemaName, String tableName) throws SQLException {
        try (ResultSet rs = metaData.getTables(null, schemaName, tableName, new String[]{"TABLE"})) {
            return rs.next();
        }
    }

    private static Set<String> existingColumns(DatabaseMetaData metaData, String schemaName, String tableName) throws SQLException {
        Set<String> columns = new LinkedHashSet<>();
        try (ResultSet rs = metaData.getColumns(null, schemaName, tableName, null)) {
            while (rs.next()) {
                columns.add(rs.getString("COLUMN_NAME").toLowerCase(Locale.ROOT));
            }
        }
        return columns;
    }

    private static Set<String> existingUniqueColumns(DatabaseMetaData metaData, String schemaName, String tableName) throws SQLException {
        Set<String> uniqueColumns = new LinkedHashSet<>();
        try (ResultSet rs = metaData.getIndexInfo(null, schemaName, tableName, true, false)) {
            while (rs.next()) {
                String column = rs.getString("COLUMN_NAME");
                if (column != null) {
                    uniqueColumns.add(column.toLowerCase(Locale.ROOT));
                }
            }
        }
        return uniqueColumns;
    }

    private static Set<String> existingForeignKeyColumns(DatabaseMetaData metaData, String schemaName, String tableName) throws SQLException {
        Set<String> fkColumns = new LinkedHashSet<>();
        try (ResultSet rs = metaData.getImportedKeys(null, schemaName, tableName)) {
            while (rs.next()) {
                fkColumns.add(rs.getString("FKCOLUMN_NAME").toLowerCase(Locale.ROOT));
            }
        }
        return fkColumns;
    }

    private static String buildCreateTable(EntityModel entity, boolean qualifyWithSchema) {
        List<String> lines = new ArrayList<>();
        for (ColumnModel column : entity.columns) {
            lines.add("    " + buildColumnDefinition(column));
        }

        List<String> primaryKeys = entity.columns.stream()
                .filter(column -> column.id)
                .map(column -> column.name)
                .toList();
        if (!primaryKeys.isEmpty()) {
            lines.add("    PRIMARY KEY (" + String.join(", ", primaryKeys) + ")");
        }

        return "CREATE TABLE IF NOT EXISTS " + entity.qualifiedTableName(qualifyWithSchema) + " (" + System.lineSeparator()
                + String.join("," + System.lineSeparator(), lines) + System.lineSeparator()
                + ");";
    }

    private static String buildAddColumn(EntityModel entity, ColumnModel column, boolean qualifyWithSchema) {
        return "ALTER TABLE " + entity.qualifiedTableName(qualifyWithSchema) + System.lineSeparator()
                + "    ADD COLUMN IF NOT EXISTS " + buildColumnDefinition(column) + ";";
    }

    private static String buildDropColumn(EntityModel entity, String columnName, boolean qualifyWithSchema) {
        return "ALTER TABLE " + entity.qualifiedTableName(qualifyWithSchema) + System.lineSeparator()
                + "    DROP COLUMN IF EXISTS " + columnName + ";";
    }

    private static String buildUniqueIndex(EntityModel entity, ColumnModel column, boolean qualifyWithSchema) {
        return "CREATE UNIQUE INDEX IF NOT EXISTS "
                + "ux_" + entity.tableName + "_" + column.name
                + " ON " + entity.qualifiedTableName(qualifyWithSchema)
                + " (" + column.name + ");";
    }

    private static String buildForeignKey(EntityModel entity, ColumnModel column, boolean qualifyWithSchema) {
        ReferenceModel reference = Objects.requireNonNull(column.reference);
        String constraintName = "fk_" + entity.tableName + "_" + column.name;
        return "ALTER TABLE " + entity.qualifiedTableName(qualifyWithSchema) + System.lineSeparator()
                + "    ADD CONSTRAINT " + constraintName
                + " FOREIGN KEY (" + column.name + ") REFERENCES "
                + reference.qualifiedTableName()
                + " (" + reference.columnName + ");";
    }

    private static String buildColumnDefinition(ColumnModel column) {
        StringBuilder builder = new StringBuilder()
                .append(column.name)
                .append(' ')
                .append(column.sqlType);

        if (column.id || !column.nullable) {
            builder.append(" NOT NULL");
        }
        if (column.unique && !column.id) {
            builder.append(" UNIQUE");
        }

        return builder.toString();
    }

    private static String resolveTenantSchema(Connection connection, String tenantSchemaOverride) throws SQLException {
        if (tenantSchemaOverride != null && !tenantSchemaOverride.isBlank()) {
            return tenantSchemaOverride.trim();
        }

        try (var statement = connection.prepareStatement(
                "select schema_name from public.tenants where schema_name is not null and schema_name <> '' order by schema_name limit 1"
        )) {
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() ? rs.getString(1) : null;
            }
        }
    }

    private static void writeMigrationIfNeeded(
            Path directory,
            String migrationName,
            List<String> statements,
            String comparedTarget
    ) throws IOException {
        if (statements.isEmpty()) {
            System.out.println("No changes detected for " + comparedTarget + ".");
            return;
        }

        Files.createDirectories(directory);
        int version = nextVersion(directory);
        Path output = directory.resolve("V" + version + "__" + migrationName + ".sql");

        String header = "-- Generated by MigrationScaffoldGenerator" + System.lineSeparator()
                + "-- Compared against: " + comparedTarget + System.lineSeparator()
                + "-- Review before applying. Additive changes are the supported path." + System.lineSeparator()
                + System.lineSeparator();
        String body = statements.stream().collect(Collectors.joining(System.lineSeparator() + System.lineSeparator()));

        Files.writeString(output, header + body + System.lineSeparator(), StandardCharsets.UTF_8);
        System.out.println("Generated migration: " + output);
    }

    private static int nextVersion(Path directory) throws IOException {
        if (!Files.exists(directory)) {
            return 1;
        }

        int maxVersion = 0;
        try (Stream<Path> stream = Files.list(directory)) {
            for (Path file : stream.filter(Files::isRegularFile).toList()) {
                Matcher matcher = VERSION_PATTERN.matcher(file.getFileName().toString());
                if (matcher.matches()) {
                    maxVersion = Math.max(maxVersion, Integer.parseInt(matcher.group(1)));
                }
            }
        }
        return maxVersion + 1;
    }

    private static String camelToSnake(String value) {
        return value.replaceAll("([a-z0-9])([A-Z])", "$1_$2").toLowerCase(Locale.ROOT);
    }

    private record EntityModel(
            Class<?> entityClass,
            String tableName,
            String schemaName,
            Scope scope,
            List<ColumnModel> columns
    ) {
        String qualifiedTableName(boolean qualifyWithSchema) {
            if (qualifyWithSchema && schemaName != null && !schemaName.isBlank()) {
                return schemaName + "." + tableName;
            }
            return tableName;
        }

        List<ColumnModel> foreignKeys() {
            return columns.stream().filter(column -> column.reference != null).toList();
        }
    }

    private record ColumnModel(
            String name,
            String sqlType,
            boolean nullable,
            boolean unique,
            boolean id,
            ReferenceModel reference,
            boolean basic
    ) {
    }

    private record ReferenceModel(
            String tableName,
            String schemaName,
            String columnName,
            Class<?> idType
    ) {
        String qualifiedTableName() {
            if (schemaName != null && !schemaName.isBlank()) {
                return schemaName + "." + tableName;
            }
            return tableName;
        }
    }

    private enum Scope {
        PUBLIC,
        TENANT
    }

    private record Arguments(String migrationName, String tenantSchemaOverride) {
        static Arguments parse(String[] args) {
            String migrationName = null;
            String tenantSchema = null;

            for (String arg : args) {
                if (arg.startsWith("--tenant-schema=")) {
                    tenantSchema = arg.substring("--tenant-schema=".length()).trim();
                    continue;
                }
                if (migrationName == null) {
                    migrationName = slugify(arg);
                }
            }

            if (migrationName == null || migrationName.isBlank()) {
                throw new IllegalArgumentException(
                        "Usage: MigrationScaffoldGenerator <migration_name> [--tenant-schema=tenant_schema]"
                );
            }

            return new Arguments(migrationName, tenantSchema);
        }

        private static String slugify(String value) {
            return value.trim()
                    .toLowerCase(Locale.ROOT)
                    .replaceAll("[^a-z0-9]+", "_")
                    .replaceAll("^_+|_+$", "");
        }
    }
}
