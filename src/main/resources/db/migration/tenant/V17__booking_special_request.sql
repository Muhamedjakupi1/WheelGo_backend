-- Compared against: tenant schemas

ALTER TABLE bookings
    ADD COLUMN IF NOT EXISTS special_request TEXT;
