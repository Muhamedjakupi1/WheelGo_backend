# WheelGo Backend

WheelGo is the backend service for a car rental platform built for the **Distributed Systems 2025/26** class project. The system follows a client-server architecture: the frontend and backend are independent applications, and all communication happens through REST APIs over HTTP/HTTPS.

This repository contains only the backend part. The frontend is expected to consume these APIs from a separate React application.

## Project Overview

The backend was created as a Spring Boot REST API that manages tenants, users, vehicles, bookings, payments, invoices, reviews, support tickets, driver license verification, caching, and database migrations.

The project was developed in phases:

1. A Spring Boot Maven project was initialized with Java 21.
2. The first REST controllers, services, repositories, and entities were created using an OOP structure.
3. PostgreSQL was added as the main database, with Spring Data JPA as the ORM layer.
4. Flyway migrations were added so database changes are versioned and repeatable.
5. Authentication and authorization were implemented with JWT tokens and Spring Security.
6. Multi-tenancy was added by separating tenant-owned data into PostgreSQL schemas.
7. Admin and user workflows were added for vehicles, bookings, payments, invoices, reviews, maintenance, and support.
8. Redis caching was added for frequently used data such as tenants, vehicles, users, and bookings.
9. Background and external processing flows were added for invoice emails, driver license AI verification with Ollama, booking status cleanup, add-on inventory release, and review eligibility.
10. Swagger/OpenAPI documentation and automated tests were added to make the API easier to inspect and verify.

## Technology Stack

- Java 21
- Spring Boot 4
- Spring Web
- Spring Security
- Spring Data JPA
- PostgreSQL
- Flyway
- Redis
- Maven
- Swagger UI / OpenAPI
- JUnit and Spring Boot Test
- Lombok
- MapStruct

## Distributed Systems Requirements

The class requirements are covered as follows:

| Requirement | Backend Implementation |
| --- | --- |
| Client-server architecture | Backend exposes REST APIs and does not depend on the frontend implementation. |
| HTTP/HTTPS communication | Controllers expose HTTP endpoints using REST conventions. |
| Minimum 20 endpoints | The backend contains more than 20 endpoints across auth, tenants, vehicles, bookings, payments, invoices, users, support, reviews, and admin modules. |
| RESTful API and framework | Implemented with Spring Boot REST controllers. |
| OOP programming | Code is organized into controllers, services, repositories, entities, DTOs, mappers, filters, and configuration classes. |
| Swagger documentation | Swagger UI is configured at `/swagger-ui.html`; API docs are available at `/api-docs`. |
| ORM and database | Spring Data JPA communicates with PostgreSQL through entity models and repositories. |
| Authentication and authorization | JWT login/signup with role-based authorization through Spring Security. |
| Middleware | Security filters handle JWT authentication and tenant schema resolution. |
| Frontend React + Context | Handled in the separate frontend repository. This backend provides the API consumed by that frontend. |
| Testing and CI/CD | Unit and API-style controller tests are included under `src/test`; these can be run by a CI pipeline with `mvn test`. |
| Minimum 20 models and migrations | The backend contains 20+ entity/model areas and Flyway migrations for public and tenant schemas. |
| Project documentation | This README documents architecture, setup, APIs, and backend logic. |
| Project management | Intended to be managed with GitHub branches, pull requests, reviews, and project tasks. |
| Git collaboration | Work should be done through branches and pull requests. |
| AI / LLM integration | Driver license verification uses an LLM vision service through the Ollama API. The same module can be adapted to OpenAI if required by deployment. |
| Caching | Redis caching is configured through Spring Cache. |
| Async/background jobs | Invoice email sending uses `@Async`; driver license verification calls an external LLM/Ollama service; scheduled jobs manage booking and inventory state. |
| Multi-tenancy | Each tenant has its own PostgreSQL schema; public data stores tenants and users. |
| Search and filtering | Vehicles, bookings, payments, users, maintenance, and admin lists support keyword filtering/search patterns. |

## Architecture

The backend follows a layered architecture:

- **Controllers** receive HTTP requests and return API responses.
- **Services** contain business logic, validation, and transactional workflows.
- **Repositories** provide database access through Spring Data JPA.
- **Entities and DTOs** define database tables and request/response payloads.
- **Mappers** convert entities into response objects.
- **Security filters** authenticate JWT tokens and resolve tenant context.
- **Flyway migrations** create and update public and tenant database schemas.

The package structure follows this separation:

```text
src/main/java/com/wheelGo
  auth/          Authentication request/response DTOs
  config/        Security, Redis, CORS, Swagger, datasource, file config
  controller/    REST API controllers
  mapper/        MapStruct entity-response mappers
  model/         Entities, request DTOs, response DTOs, enums
  repository/    Spring Data repositories
  schema/        Multi-tenant schema utilities
  security/      JWT, principals, auth filters, error handlers
  service/       Business logic
  tools/         Migration and enum utilities
```

## Multi-Tenancy

WheelGo supports multiple rental companies in the same backend.

Tenant data is separated with PostgreSQL schemas:

- The `public` schema stores shared data such as tenants and users.
- Each tenant has its own schema, for example `wheelgo_demo` or `super_admin_tenant`.
- Tenant-specific tables such as vehicles, bookings, payments, invoices, reviews, and settings live inside the tenant schema.

When a request is authenticated, the JWT contains the tenant slug. `TenantSchemaFilter` reads the authenticated user, finds the tenant schema, and stores it in `TenantContext`. `TenantAwareDataSource` then applies:

```sql
SET search_path TO "tenant_schema", public
```

This means the same repository code automatically reads and writes data inside the current tenant schema.

Super admin users can manage tenants and can optionally target a tenant through the `X-Tenant-Slug` header.

## Authentication and Authorization

Authentication is handled through JWT.

The two most important endpoints for the frontend are:

### `POST /api/auth/signup/{tenantSlug}`

Registers a normal customer inside a specific tenant.

Main logic:

- Validates that the tenant slug exists and is active.
- Blocks signup for reserved tenant slugs.
- Validates email, first name, last name, phone number, and password policy.
- Checks that the same email does not already exist inside that tenant.
- Creates a `USER` account in the public users table.
- Creates the user's profile and initial settings inside the tenant schema.
- Writes an audit log entry.
- Returns a JWT token and user/tenant details.

The frontend uses this endpoint when a customer signs up for a specific rental company.

### `POST /api/auth/login/{tenantSlug}`

Logs a user into a specific tenant.

Main logic:

- Finds the tenant by slug.
- Checks that the tenant and user are active.
- Looks up the user by email and tenant ID.
- Verifies the BCrypt password hash.
- Creates a JWT containing user ID, role, tenant ID, and tenant slug.
- Writes a login audit log.
- Returns the token and user context.

The frontend stores the token and sends it in the `Authorization: Bearer <token>` header for protected API calls.

Roles are enforced through Spring Security:

- `USER` can manage their own profile, bookings, payments, reviews, support tickets, and driver license.
- `ADMIN` can manage tenant-owned resources such as vehicles, bookings, users, locations, add-ons, maintenance, payments, and settings.
- `SUPER_ADMIN` can manage tenants and impersonation flows.

## Main API Areas

The backend exposes many endpoints. The most important groups are:

| Area | Endpoints |
| --- | --- |
| Auth | `POST /api/auth/login/{tenantSlug}`, `POST /api/auth/signup/{tenantSlug}` |
| Public tenant lookup | `GET /api/public/tenants/{slug}` |
| Tenant management | `/api/super-admin/tenants` |
| Impersonation | `/api/super-admin/impersonation` |
| Vehicles | `/api/v1/vehicles`, `/api/v1/admin/vehicles` |
| Vehicle categories | `/api/v1/admin/vehicle-categories` |
| Vehicle images | `/api/v1/admin/vehicle-images` |
| Bookings | `/api/v1/bookings`, `/api/v1/admin/bookings` |
| Payments | `/api/v1/payments`, `/api/v1/admin/payments` |
| Invoices | `/api/v1/invoices` |
| Driver license | `/api/driver-license` |
| User profile | `/api/v1/user-profile`, `/api/user-profile` |
| User settings | `/api/user-settings` |
| Admin users | `/api/v1/admin/users` |
| Tenant settings | `/api/v1/tenant-settings`, `/api/v1/admin/tenant-settings` |
| Locations | `/api/v1/admin/locations` |
| Maintenance | `/api/v1/admin/maintenances` |
| Add-ons | `/api/v1/addons`, `/api/v1/admin/addons` |
| Promotions | `/api/v1/admin/promotions` |
| Reviews | `/api/v1/reviews`, `/api/v1/admin/reviews` |
| Support tickets | `/api/v1/support/tickets`, `/api/v1/admin/support/tickets` |
| Audit logs | `/api/v1/audit-logs` |

## Booking Flow

Bookings are one of the central parts of the backend.

User endpoint:

```http
POST /api/v1/bookings
GET  /api/v1/bookings/me
```

Admin endpoints:

```http
GET    /api/v1/admin/bookings
PATCH  /api/v1/admin/bookings/{id}
PATCH  /api/v1/admin/bookings/{id}/confirm
PATCH  /api/v1/admin/bookings/{id}/reject
DELETE /api/v1/admin/bookings/{id}
```

When a user creates a booking, the backend:

1. Checks that the authenticated user exists.
2. Requires a verified and non-expired driver license.
3. Loads the selected vehicle.
4. Rejects inactive vehicles.
5. Validates the start and end dates.
6. Checks for date conflicts with confirmed or active bookings.
7. Checks vehicle maintenance availability.
8. Calculates total rental days.
9. Calculates base price from vehicle daily rate.
10. Resolves selected add-ons such as baby seat or Bluetooth.
11. Reserves managed add-on inventory.
12. Creates the booking with `PENDING` status.
13. Saves booking add-on rows.
14. Evicts affected Redis cache entries.

Admin users can confirm, reject, update, or delete bookings.

Important status behavior:

- `PENDING`: booking was requested by the user and waits for admin decision.
- `CONFIRMED`: admin approved the booking.
- `ACTIVE`: booking is currently in use.
- `COMPLETED`: booking ended successfully.
- `CANCELLED`: booking was rejected or cancelled.

Scheduled background jobs also help keep bookings consistent:

- Finished pending bookings are cancelled.
- Finished confirmed/active bookings are completed.
- Add-on inventory is released when bookings finish or are cancelled.
- Completed bookings become eligible for reviews.
- Vehicle status is synced with booking and maintenance state.

## Payment and Invoice Flow

Payment endpoints:

```http
POST /api/v1/payments/pay
GET  /api/v1/payments/me
GET  /api/v1/payments/booking/{bookingId}
GET  /api/v1/admin/payments
PATCH /api/v1/admin/payments/{id}/confirm
PATCH /api/v1/admin/payments/{id}/refund
```

Payment logic:

- A user can pay only for their own booking.
- If the request does not include an amount, the backend calculates the outstanding amount.
- Promotion codes can be applied before payment.
- Card payments are treated as paid immediately by default.
- Cash payments start as pending and must be confirmed by an admin.
- Paid payments generate or reuse an invoice.
- Invoice PDFs are generated and stored.
- Invoice emails are sent asynchronously after the transaction commits.
- Payments can be refunded only when the payment is paid and the booking is cancelled.

Invoice endpoints:

```http
POST /api/v1/invoices
GET  /api/v1/invoices/booking/{bookingId}/pdf
POST /api/v1/invoices/booking/{bookingId}/email
```

Invoices connect bookings, payments, generated PDF files, and customer email delivery.

## Driver License and AI Verification

Before booking, users must verify their driver license.

Endpoints:

```http
GET  /api/driver-license/me
PUT  /api/driver-license/me
POST /api/driver-license/me/front-image
POST /api/driver-license/me/back-image
POST /api/driver-license/me/verify
```

The verification process:

1. User saves license number, issuing country, and expiry date.
2. User uploads front and back license images.
3. The backend stores the files under the upload directory.
4. The verification service sends both images to an LLM vision endpoint.
5. The LLM returns a structured JSON decision.
6. The backend stores whether the license is verified.
7. Booking creation is blocked until the license is verified and not expired.

This is also treated as an asynchronous-style backend process because the API delegates the heavy document analysis to an external LLM service instead of doing the full verification locally inside the database transaction logic. The backend prepares the image payloads, sends them to Ollama, waits for the structured result, and then updates the driver's license verification state.

Current implementation uses an Ollama-compatible local API:

```properties
app.ollama.driver-license.api-url=http://localhost:11434/api/chat
app.ollama.driver-license.model=granite3.2-vision
```

If the class requirement specifically requires OpenAI, this module is the place where the provider can be changed from Ollama to OpenAI while keeping the same controller and service flow.

## Vehicles, Maintenance, and Add-ons

Admins manage the rental fleet through vehicle, category, image, location, maintenance, and add-on APIs.

Vehicle logic includes:

- Vehicle listing for customers.
- Admin CRUD for vehicles.
- Filtering/search by fields such as vehicle name, location, category, transmission, and status.
- Image upload and primary image selection.
- Status syncing based on bookings and maintenance.

Maintenance logic prevents users from booking vehicles that are currently under maintenance. If a maintenance record marks the car unavailable until a future date, booking creation returns a conflict message explaining when the vehicle becomes available.

Add-ons support optional booking items such as baby seats or Bluetooth. Managed add-ons have inventory quantity. When a booking is created, the backend reserves inventory. When the booking is cancelled or completed, the inventory is released.

## Reviews

Users can review vehicles only after eligible completed bookings.

The backend:

- Marks completed bookings as review eligible.
- Prevents reviews for non-eligible bookings.
- Stores vehicle review scores and comments.
- Allows admins to inspect all reviews.

## Support Tickets

Support tickets allow communication between users and admins.

Users can:

- Create support tickets.
- View their own tickets.
- Send and view ticket messages.

Admins can:

- View tenant support tickets.
- Update status or priority.
- Reply to messages.

## Audit Logs

Important actions such as login, create, update, and delete events are recorded in audit logs. This helps track what happened inside a tenant, especially for admin and super-admin operations.

## Database and Migrations

Flyway is used for database migrations.

Public migrations:

```text
src/main/resources/db/migration/public
```

Tenant migrations:

```text
src/main/resources/db/migration/tenant
```

Startup behavior:

1. Flyway migrates the shared `public` schema.
2. The backend loads tenants from `public.tenants`.
3. Tenant migrations are applied to every tenant schema.
4. When a new tenant is created, its schema is created and migrated immediately.

The project contains more than 20 model/entity areas, including:

- tenants
- users
- user profiles
- user settings
- tenant settings
- vehicles
- vehicle categories
- vehicle images
- locations
- bookings
- booking add-ons
- payments
- invoices
- promotions
- reviews
- support tickets
- ticket messages
- maintenance records
- driver licenses
- audit logs
- notifications
- chat sessions
- chat messages
- add-ons

## Caching

Redis is configured through Spring Cache.

The backend caches frequently used data such as:

- tenant lists
- user lists
- vehicle lists and details
- booking lists

Services evict cache entries when data changes. For example, when a booking is created or updated, booking and vehicle cache entries are cleared so the next request receives fresh data.

## Background Jobs

The backend uses asynchronous and scheduled work:

- `InvoiceEmailJobService` sends invoice emails asynchronously with `@Async`.
- Driver license verification sends uploaded front/back license images to the Ollama LLM vision endpoint and stores the AI result as the user's verification state.
- `BookingService` periodically releases finished add-on inventory.
- `BookingService` periodically marks completed bookings as review eligible.
- Booking status and vehicle status are synchronized automatically.

## Swagger

After the backend starts, Swagger UI is available at:

```text
http://localhost:8080/swagger-ui.html
```

OpenAPI docs are available at:

```text
http://localhost:8080/api-docs
```

## Local Setup

### Prerequisites

- Java 21+
- Maven or Maven Wrapper
- PostgreSQL
- Redis
- Docker Desktop, if using the included `compose.yaml` for Redis

The application expects these default services:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/wheelgo_db
spring.datasource.username=postgres
spring.datasource.password=123
spring.data.redis.host=localhost
spring.data.redis.port=6379
```

For a real deployment, secrets such as database passwords, JWT secrets, and email credentials should be moved to environment variables.

### Start Redis with Docker Compose

```powershell
docker compose up -d
```

The included `compose.yaml` starts Redis on port `6379`.

### Run the Backend

With Maven:

```powershell
mvn spring-boot:run
```

With the Maven wrapper:

```powershell
.\mvnw.cmd spring-boot:run
```

If Docker Desktop is not running and Spring Boot tries to start Docker Compose automatically, run:

```powershell
mvn spring-boot:run '-Dspring-boot.run.arguments=--spring.docker.compose.enabled=false'
```

## Tests

Run the test suite with:

```powershell
mvn test
```

or:

```powershell
.\mvnw.cmd test
```

The tests cover controllers and service logic for authentication, bookings, payments-related workflows, users, tenant settings, vehicles, add-ons, driver licenses, support modules, and other backend behavior.

## Useful Migration Commands

Generate enum SQL:

```powershell
.\mvnw.cmd -q -DskipTests compile
java -cp target\classes com.wheelGo.tools.EnumSqlGenerator
```

Generate a migration scaffold:

```powershell
.\scripts\generate-migration.ps1 add_new_change
```

Generate and apply a migration:

```powershell
.\scripts\generate-migration.ps1 add_new_change -Apply
```

Apply existing migrations:

```powershell
.\scripts\apply-migrations.ps1
```

## Git Workflow

Recommended workflow for collaboration:

1. Create a feature branch from `main`.
2. Make the backend change.
3. Run tests.
4. Push the branch to GitHub.
5. Open a pull request.
6. Request code review before merging.

This README was prepared on the branch:

```text
backend-readme-docs
```
