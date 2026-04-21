package com.wheelGo.tools;

import com.wheelGo.model.enums.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class EnumSqlGenerator {

    private static final List<Class<? extends Enum<?>>> ENUM_CLASSES = List.of(
            Role.class, Plan.class, BookingStatus.class, PaymentStatus.class,
            PaymentMethod.class, FuelType.class, Transmission.class,
            VehicleStatus.class, DiscountType.class, AddonType.class,
            TicketStatus.class, TicketPriority.class, NotificationChannel.class,
            ChatRole.class, AuditAction.class, MaintenanceType.class
    );

    public static void main(String[] args) throws IOException {
        Path publicDir = Paths.get("src/main/resources/db/migration/public");
        Path tenantDir = Paths.get("src/main/resources/db/migration/tenant");

        Files.createDirectories(publicDir);
        Files.createDirectories(tenantDir);

        processScope(publicDir, PgEnumScope.PUBLIC);
        processScope(tenantDir, PgEnumScope.TENANT);
    }

    private static void processScope(Path dir, PgEnumScope scope) throws IOException {
        String newContent = buildSqlBody(scope);
        Optional<Path> lastFile = getLastMigrationFile(dir);

        boolean shouldGenerate = true;

        if (lastFile.isPresent()) {
            String oldContent = Files.readString(lastFile.get(), StandardCharsets.UTF_8);
            // Krahasojmë vetëm trupin e SQL-it (duke hequr komentet e gjeneruara automatikisxt)
            if (extractBody(oldContent).equals(extractBody(newContent))) {
                shouldGenerate = false;
            }
        }

        if (shouldGenerate) {
            int nextVersion = getNextVersionNumber(lastFile);
            String fileName = String.format("V%d__sync_%s_enums.sql", nextVersion, scope.name().toLowerCase());
            Files.writeString(dir.resolve(fileName), newContent, StandardCharsets.UTF_8);
            System.out.println("✅ Ndryshim i detektuar! U gjenerua: " + fileName);
        } else {
            System.out.println("ℹ️ Nuk ka ndryshime për scope: " + scope.name() + ". Skipped.");
        }
    }

    private static String buildSqlBody(PgEnumScope scope) {
        StringBuilder sb = new StringBuilder();
        sb.append("-- Generated Scope: ").append(scope.name()).append("\n");

        ENUM_CLASSES.stream()
                .filter(e -> metadata(e).scope() == scope)
                .sorted(Comparator.comparing(e -> metadata(e).value()))
                .forEach(enumClass -> {
                    PgEnumType meta = metadata(enumClass);
                    String fullName = (meta.scope().schemaName() == null) ? meta.value() : meta.scope().schemaName() + "." + meta.value();

                    for (Object constant : enumClass.getEnumConstants()) {
                        String val = ((Enum<?>) constant).name();
                        sb.append(String.format(
                                "DO $$ BEGIN\n" +
                                        "    BEGIN ALTER TYPE %s ADD VALUE '%s'; EXCEPTION WHEN duplicate_object THEN null; END;\n" +
                                        "END $$;\n", fullName, val));
                    }
                });
        return sb.toString();
    }

    private static Optional<Path> getLastMigrationFile(Path dir) throws IOException {
        try (Stream<Path> stream = Files.list(dir)) {
            return stream
                    .filter(p -> p.getFileName().toString().startsWith("V") && p.getFileName().toString().contains("__"))
                    .max(Comparator.comparing(p -> getVersionFromPath(p)));
        }
    }

    private static int getNextVersionNumber(Optional<Path> lastFile) {
        return lastFile.map(path -> getVersionFromPath(path) + 1).orElse(0);
    }

    private static int getVersionFromPath(Path path) {
        String name = path.getFileName().toString();
        return Integer.parseInt(name.substring(1, name.indexOf("__")));
    }

    private static String extractBody(String fullSql) {
        return fullSql.lines()
                .filter(line -> !line.trim().startsWith("--"))
                .collect(Collectors.joining("\n")).trim();
    }

    private static PgEnumType metadata(Class<? extends Enum<?>> enumClass) {
        PgEnumType metadata = enumClass.getAnnotation(PgEnumType.class);
        if (metadata == null) throw new IllegalStateException("Missing @PgEnumType on " + enumClass.getName());
        return metadata;
    }
}