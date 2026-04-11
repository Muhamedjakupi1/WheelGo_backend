CREATE TABLE IF NOT EXISTS tenant_settings (
                                               id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    currency    VARCHAR(10)  NOT NULL DEFAULT 'EUR',
    timezone    VARCHAR(50)  NOT NULL DEFAULT 'UTC',
    logo_url    TEXT,
    theme_color VARCHAR(20)  DEFAULT '#1A73E8',
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP    NOT NULL DEFAULT NOW()
    );

CREATE TABLE IF NOT EXISTS user_profiles (
    id             UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id        UUID        NOT NULL REFERENCES public.users(id) ON DELETE CASCADE,
    first_name     VARCHAR(80) NOT NULL,
    last_name      VARCHAR(80) NOT NULL,
    phone          VARCHAR(30),
    avatar_url     TEXT,
    date_of_birth  DATE,
    address        TEXT,
    city           VARCHAR(80),
    country        VARCHAR(80),
    created_at     TIMESTAMP   NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMP   NOT NULL DEFAULT NOW(),
    UNIQUE (user_id)
    );

CREATE TABLE IF NOT EXISTS driver_licenses (
    id               UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id          UUID        NOT NULL REFERENCES public.users(id) ON DELETE CASCADE,
    license_number   VARCHAR(60) NOT NULL,
    issuing_country  VARCHAR(60) NOT NULL,
    expiry_date      DATE        NOT NULL,
    front_image_url  TEXT,
    back_image_url   TEXT,
    verified_at      TIMESTAMP,
    created_at       TIMESTAMP   NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMP   NOT NULL DEFAULT NOW(),
    UNIQUE (user_id)
    );

CREATE TABLE IF NOT EXISTS locations (
    id         UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    name       VARCHAR(100) NOT NULL,
    address    TEXT         NOT NULL,
    city       VARCHAR(80)  NOT NULL,
    country    VARCHAR(80)  NOT NULL DEFAULT 'Kosovo',
    latitude   NUMERIC(10,7),
    longitude  NUMERIC(10,7),
    phone      VARCHAR(30),
    is_active  BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP    NOT NULL DEFAULT NOW()
    );

CREATE TABLE IF NOT EXISTS vehicle_categories (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(60)  NOT NULL UNIQUE,
    description TEXT,
    image_url   TEXT,
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP    NOT NULL DEFAULT NOW()
    );

CREATE TABLE IF NOT EXISTS vehicles (
    id           UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    category_id  UUID          NOT NULL REFERENCES vehicle_categories(id),
    location_id  UUID          REFERENCES locations(id),
    plate_number VARCHAR(20)   NOT NULL UNIQUE,
    make         VARCHAR(60)   NOT NULL,
    model        VARCHAR(60)   NOT NULL,
    year         SMALLINT      NOT NULL,
    color        VARCHAR(40),
    vin          VARCHAR(20)   UNIQUE,
    fuel_type    VARCHAR(20)   NOT NULL DEFAULT 'PETROL',
    transmission VARCHAR(20)   NOT NULL DEFAULT 'MANUAL',
    seats        SMALLINT      NOT NULL DEFAULT 5,
    daily_rate   NUMERIC(10,2) NOT NULL,
    status       VARCHAR(20)   NOT NULL DEFAULT 'AVAILABLE',
    mileage      INT           NOT NULL DEFAULT 0,
    created_at   TIMESTAMP     NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMP     NOT NULL DEFAULT NOW()
    );

CREATE TABLE IF NOT EXISTS vehicle_images (
    id          UUID      PRIMARY KEY DEFAULT gen_random_uuid(),
    vehicle_id  UUID      NOT NULL REFERENCES vehicles(id) ON DELETE CASCADE,
    url         TEXT      NOT NULL,
    is_primary  BOOLEAN   NOT NULL DEFAULT FALSE,
    uploaded_at TIMESTAMP NOT NULL DEFAULT NOW()
    );

CREATE TABLE IF NOT EXISTS maintenance_records (
    id            UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    vehicle_id    UUID          NOT NULL REFERENCES vehicles(id) ON DELETE CASCADE,
    type          VARCHAR(60)   NOT NULL,   -- OIL_CHANGE, TIRE, INSPECTION, REPAIR
    description   TEXT,
    cost          NUMERIC(10,2),
    performed_at  TIMESTAMP     NOT NULL,
    next_due_at   TIMESTAMP,
    performed_by  VARCHAR(100),
    created_at    TIMESTAMP     NOT NULL DEFAULT NOW()
    );

CREATE TABLE IF NOT EXISTS addons (
    id          UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(80)   NOT NULL,
    description TEXT,
    price       NUMERIC(10,2) NOT NULL,
    type        VARCHAR(40),              -- DAILY, ONE_TIME
    is_active   BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP     NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP     NOT NULL DEFAULT NOW()
    );

CREATE TABLE IF NOT EXISTS promotions (
    id             UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    code           VARCHAR(40)   NOT NULL UNIQUE,
    discount_type  VARCHAR(20)   NOT NULL DEFAULT 'PERCENTAGE',  -- PERCENTAGE, FIXED
    discount_value NUMERIC(10,2) NOT NULL,
    max_uses       INT,
    uses_count     INT           NOT NULL DEFAULT 0,
    valid_from     TIMESTAMP     NOT NULL,
    valid_until    TIMESTAMP,
    is_active      BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMP     NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMP     NOT NULL DEFAULT NOW()
    );

CREATE TABLE IF NOT EXISTS bookings (
    id                  UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id             UUID          NOT NULL REFERENCES public.users(id),
    vehicle_id          UUID          NOT NULL REFERENCES vehicles(id),
    pickup_location_id  UUID          NOT NULL REFERENCES locations(id),
    dropoff_location_id UUID          NOT NULL REFERENCES locations(id),
    promotion_id        UUID          REFERENCES promotions(id),
    start_date          TIMESTAMP     NOT NULL,
    end_date            TIMESTAMP     NOT NULL,
    total_days          INT           NOT NULL,
    base_price          NUMERIC(10,2) NOT NULL,
    discount_amount     NUMERIC(10,2) NOT NULL DEFAULT 0,
    addon_price         NUMERIC(10,2) NOT NULL DEFAULT 0,
    total_price         NUMERIC(10,2) NOT NULL,
    status              VARCHAR(20)   NOT NULL DEFAULT 'PENDING',  -- PENDING, CONFIRMED, ACTIVE, COMPLETED, CANCELLED
    notes               TEXT,
    created_at          TIMESTAMP     NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP     NOT NULL DEFAULT NOW()
    );

CREATE TABLE IF NOT EXISTS booking_addons (
    id             UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    booking_id     UUID          NOT NULL REFERENCES bookings(id) ON DELETE CASCADE,
    addon_id       UUID          NOT NULL REFERENCES addons(id),
    quantity       SMALLINT      NOT NULL DEFAULT 1,
    price_snapshot NUMERIC(10,2) NOT NULL,
    created_at     TIMESTAMP     NOT NULL DEFAULT NOW()
    );

CREATE TABLE IF NOT EXISTS payments (
    id          UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    booking_id  UUID          NOT NULL REFERENCES bookings(id) ON DELETE CASCADE,
    amount      NUMERIC(10,2) NOT NULL,
    currency    VARCHAR(10)   NOT NULL DEFAULT 'EUR',
    method      VARCHAR(30)   NOT NULL,   -- CARD, CASH, BANK_TRANSFER
    status      VARCHAR(20)   NOT NULL DEFAULT 'PENDING',  -- PENDING, PAID, FAILED, REFUNDED
    gateway_ref VARCHAR(150),
    paid_at     TIMESTAMP,
    created_at  TIMESTAMP     NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP     NOT NULL DEFAULT NOW()
    );

CREATE TABLE IF NOT EXISTS invoices (
   id             UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    booking_id     UUID        NOT NULL REFERENCES bookings(id) ON DELETE CASCADE,
    invoice_number VARCHAR(40) NOT NULL UNIQUE,
    pdf_url        TEXT,
    issued_at      TIMESTAMP   NOT NULL DEFAULT NOW(),
    due_at         TIMESTAMP,
    created_at     TIMESTAMP   NOT NULL DEFAULT NOW(),
    UNIQUE (booking_id)
    );

CREATE TABLE IF NOT EXISTS reviews (
    id         UUID      PRIMARY KEY DEFAULT gen_random_uuid(),
    booking_id UUID      NOT NULL REFERENCES bookings(id) ON DELETE CASCADE,
    user_id    UUID      NOT NULL REFERENCES public.users(id),
    vehicle_id UUID      NOT NULL REFERENCES vehicles(id),
    rating     SMALLINT  NOT NULL CHECK (rating BETWEEN 1 AND 5),
    comment    TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE (booking_id)
    );

CREATE TABLE IF NOT EXISTS support_tickets (
    id         UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    UUID         NOT NULL REFERENCES public.users(id),
    booking_id UUID         REFERENCES bookings(id),
    subject    VARCHAR(150) NOT NULL,
    status     VARCHAR(20)  NOT NULL DEFAULT 'OPEN',    -- OPEN, IN_PROGRESS, RESOLVED, CLOSED
    priority   VARCHAR(20)  NOT NULL DEFAULT 'NORMAL',  -- LOW, NORMAL, HIGH, URGENT
    created_at TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP    NOT NULL DEFAULT NOW()
    );

CREATE TABLE IF NOT EXISTS ticket_messages (
    id        UUID      PRIMARY KEY DEFAULT gen_random_uuid(),
    ticket_id UUID      NOT NULL REFERENCES support_tickets(id) ON DELETE CASCADE,
    sender_id UUID      NOT NULL REFERENCES public.users(id),
    message   TEXT      NOT NULL,
    is_staff  BOOLEAN   NOT NULL DEFAULT FALSE,
    sent_at   TIMESTAMP NOT NULL DEFAULT NOW()
    );

CREATE TABLE IF NOT EXISTS notifications (
    id      UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID         NOT NULL REFERENCES public.users(id) ON DELETE CASCADE,
    type    VARCHAR(50)  NOT NULL,    -- BOOKING_CONFIRMED, PAYMENT_RECEIVED, etc.
    title   VARCHAR(150) NOT NULL,
    body    TEXT         NOT NULL,
    channel VARCHAR(20)  NOT NULL DEFAULT 'EMAIL',  -- EMAIL, PUSH, SMS
    is_read BOOLEAN      NOT NULL DEFAULT FALSE,
    sent_at TIMESTAMP    NOT NULL DEFAULT NOW()
    );

CREATE TABLE IF NOT EXISTS chat_sessions (
    id         UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    UUID         NOT NULL REFERENCES public.users(id),
    title      VARCHAR(150),
    started_at TIMESTAMP    NOT NULL DEFAULT NOW(),
    ended_at   TIMESTAMP
    );

CREATE TABLE IF NOT EXISTS chat_messages (
    id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id  UUID         NOT NULL REFERENCES chat_sessions(id) ON DELETE CASCADE,
    role        VARCHAR(20)  NOT NULL,   -- USER, ASSISTANT, SYSTEM
    content     TEXT         NOT NULL,
    tokens_used INT,
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW()
    );

CREATE TABLE IF NOT EXISTS audit_logs (
    id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID         REFERENCES public.users(id),
    action      VARCHAR(60)  NOT NULL,   -- CREATE, UPDATE, DELETE, LOGIN, etc.
    entity_type VARCHAR(60)  NOT NULL,   -- Booking, Vehicle, User, etc.
    entity_id   UUID,
    old_values  JSONB,
    new_values  JSONB,
    ip_address  VARCHAR(45),
    user_agent  TEXT,
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW()
    );

-- ============================================================
-- INDEXES
-- ============================================================
CREATE INDEX IF NOT EXISTS idx_bookings_user_id    ON bookings(user_id);
CREATE INDEX IF NOT EXISTS idx_bookings_vehicle_id ON bookings(vehicle_id);
CREATE INDEX IF NOT EXISTS idx_bookings_status     ON bookings(status);
CREATE INDEX IF NOT EXISTS idx_bookings_start_date ON bookings(start_date);
CREATE INDEX IF NOT EXISTS idx_vehicles_status     ON vehicles(status);
CREATE INDEX IF NOT EXISTS idx_vehicles_category   ON vehicles(category_id);
CREATE INDEX IF NOT EXISTS idx_payments_booking    ON payments(booking_id);
CREATE INDEX IF NOT EXISTS idx_payments_status     ON payments(status);
CREATE INDEX IF NOT EXISTS idx_reviews_vehicle     ON reviews(vehicle_id);
CREATE INDEX IF NOT EXISTS idx_notifications_user  ON notifications(user_id);
CREATE INDEX IF NOT EXISTS idx_notifications_read  ON notifications(is_read);
CREATE INDEX IF NOT EXISTS idx_audit_entity        ON audit_logs(entity_type, entity_id);
CREATE INDEX IF NOT EXISTS idx_chat_messages_sess  ON chat_messages(session_id);
CREATE INDEX IF NOT EXISTS idx_tickets_user        ON support_tickets(user_id);
CREATE INDEX IF NOT EXISTS idx_maintenance_vehicle ON maintenance_records(vehicle_id);
CREATE INDEX IF NOT EXISTS idx_vehicles_search     ON vehicles
    USING gin(to_tsvector('english', make || ' ' || model || ' ' || COALESCE(color, '')));