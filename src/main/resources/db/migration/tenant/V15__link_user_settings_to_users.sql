ALTER TABLE user_settings
    ADD COLUMN IF NOT EXISTS user_id UUID;

ALTER TABLE user_settings
    ALTER COLUMN id SET DEFAULT gen_random_uuid(),
    ALTER COLUMN password_changed SET DEFAULT FALSE,
    ALTER COLUMN created_at SET DEFAULT NOW(),
    ALTER COLUMN updated_at SET DEFAULT NOW();

UPDATE user_settings
SET created_at = COALESCE(created_at, NOW()),
    updated_at = COALESCE(updated_at, NOW()),
    password_changed = COALESCE(password_changed, FALSE)
WHERE created_at IS NULL
   OR updated_at IS NULL;

CREATE UNIQUE INDEX IF NOT EXISTS ux_user_settings_user_id
    ON user_settings(user_id)
    WHERE user_id IS NOT NULL;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_user_settings_user'
          AND conrelid = 'user_settings'::regclass
    ) THEN
        ALTER TABLE user_settings
            ADD CONSTRAINT fk_user_settings_user
            FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE
            NOT VALID;
    END IF;
END $$;
