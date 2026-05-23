ALTER TABLE bookings
    ALTER COLUMN review_eligible SET DEFAULT FALSE;

UPDATE bookings
SET review_eligible = FALSE
WHERE review_eligible IS NULL;

ALTER TABLE bookings
    ALTER COLUMN review_eligible SET NOT NULL;
