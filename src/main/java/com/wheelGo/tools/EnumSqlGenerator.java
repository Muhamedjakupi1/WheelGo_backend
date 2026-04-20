package com.wheelGo.tools;

import com.wheelGo.model.enums.AddonType;
import com.wheelGo.model.enums.AuditAction;
import com.wheelGo.model.enums.BookingStatus;
import com.wheelGo.model.enums.ChatRole;
import com.wheelGo.model.enums.DiscountType;
import com.wheelGo.model.enums.FuelType;
import com.wheelGo.model.enums.MaintenanceType;
import com.wheelGo.model.enums.NotificationChannel;
import com.wheelGo.model.enums.PaymentMethod;
import com.wheelGo.model.enums.PaymentStatus;
import com.wheelGo.model.enums.PgEnumScope;
import com.wheelGo.model.enums.PgEnumType;
import com.wheelGo.model.enums.Plan;
import com.wheelGo.model.enums.Role;
import com.wheelGo.model.enums.TicketPriority;
import com.wheelGo.model.enums.TicketStatus;
import com.wheelGo.model.enums.Transmission;
import com.wheelGo.model.enums.VehicleStatus;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public final class EnumSqlGenerator {

    private static final List<Class<? extends Enum<?>>> ENUM_CLASSES = List.of(
            Role.class,
            Plan.class,
            BookingStatus.class,
            PaymentStatus.class,
            PaymentMethod.class,
            FuelType.class,
            Transmission.class,
            VehicleStatus.class,
            DiscountType.class,
            AddonType.class,
            TicketStatus.class,
            TicketPriority.class,
            NotificationChannel.class,
            ChatRole.class,
            AuditAction.class,
            MaintenanceType.class
    );

    private EnumSqlGenerator() {
    }

    public static void main(String[] args) throws IOException {
        Path outputDir = args.length > 0
                ? Paths.get(args[0])
                : Paths.get("target", "generated-enums");

        Files.createDirectories(outputDir);
        writeFile(outputDir.resolve("public-enums.sql"), PgEnumScope.PUBLIC);
        writeFile(outputDir.resolve("tenant-enums.sql"), PgEnumScope.TENANT);
    }

    private static void writeFile(Path file, PgEnumScope scope) throws IOException {
        String content = buildSql(scope);
        Files.writeString(file, content, StandardCharsets.UTF_8);
    }

    private static String buildSql(PgEnumScope scope) {
        List<Class<? extends Enum<?>>> enums = ENUM_CLASSES.stream()
                .filter(enumClass -> metadata(enumClass).scope() == scope)
                .sorted(Comparator.comparing(enumClass -> metadata(enumClass).value()))
                .toList();

        String body = enums.stream()
                .map(EnumSqlGenerator::createTypeStatement)
                .collect(Collectors.joining(System.lineSeparator() + System.lineSeparator()));

        return "-- Generated from com.wheelGo.model.enums" + System.lineSeparator()
                + "-- Scope: " + scope.name() + System.lineSeparator()
                + body + System.lineSeparator();
    }

    private static String createTypeStatement(Class<? extends Enum<?>> enumClass) {
        PgEnumType metadata = metadata(enumClass);
        String qualifiedTypeName = metadata.scope().schemaName() == null
                ? metadata.value()
                : metadata.scope().schemaName() + "." + metadata.value();

        String values = List.of(enumClass.getEnumConstants()).stream()
                .map(Enum::name)
                .map(value -> "'" + value + "'")
                .collect(Collectors.joining(", "));

        return "CREATE TYPE " + qualifiedTypeName + " AS ENUM (" + values + ");";
    }

    private static PgEnumType metadata(Class<? extends Enum<?>> enumClass) {
        PgEnumType metadata = enumClass.getAnnotation(PgEnumType.class);
        if (metadata == null) {
            throw new IllegalStateException("Missing @PgEnumType on " + enumClass.getName());
        }
        return metadata;
    }
}
