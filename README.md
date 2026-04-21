# WheelGo_backend
Backend service for the WheelGo platform, handling business logic, API endpoints, and database operations.

Generate PostgreSQL enum SQL from `src/main/java/com/wheelGo/model/enums` with:

```powershell
.\mvnw.cmd -q -DskipTests compile
java -cp target\classes com.wheelGo.tools.EnumSqlGenerator
```

This writes:
- `target/generated-enums/public-enums.sql`
- `target/generated-enums/tenant-enums.sql`

Tenant schema migration flow:

- On app startup, Flyway migrates the shared `public` schema from `src/main/resources/db/migration/public`.
- Then `TenantSchemaStartupMigrator` loads all rows from `public.tenants` and applies `src/main/resources/db/migration/tenant` to each tenant schema.
- When a new tenant is created, `TenantSchemaService.createSchemaForTenant(...)` creates the schema and applies the same tenant migrations immediately.

When entities change:

1. Update the entity classes.
2. Create a new SQL migration under:
   - `src/main/resources/db/migration/public` for shared `public` objects
   - `src/main/resources/db/migration/tenant` for tenant-owned objects
3. Restart the app.
4. Shared migrations run once on `public`, and tenant migrations run for every schema listed in `public.tenants`.

Tenant migrations are what upgrade existing tenant schemas. Entity changes alone do not alter the database while `spring.jpa.hibernate.ddl-auto=none`.

Automatic migration scaffolding:

```powershell
.\scripts\generate-migration.ps1 add_just_testing_to_users
```

To generate and apply immediately without relying on app restart:

```powershell
.\scripts\generate-migration.ps1 add_just_testing_to_users -Apply
```

This compares the current entities against the current PostgreSQL database and writes new Flyway files into:
- `src/main/resources/db/migration/public` for `public` tables
- `src/main/resources/db/migration/tenant` for tenant tables

Notes:
- It is designed for additive changes such as new columns, new tables, new single-column unique constraints, and new foreign keys.
- It compares tenant entities against the first schema listed in `public.tenants`, or a specific one if you pass `-TenantSchema your_schema`.
- Review the generated SQL before running the app.
- `-Apply` runs Flyway directly for `public` and all tenant schemas after generating the files.

Applying existing migrations after pulling latest code:

```powershell
.\scripts\apply-migrations.ps1
```

Use this when the migration files already exist in git and you just want your local database updated to the latest version.
