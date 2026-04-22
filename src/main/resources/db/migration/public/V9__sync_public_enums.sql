-- Generated Scope: PUBLIC
DO $$ BEGIN
    BEGIN ALTER TYPE public.tenant_plan ADD VALUE 'FREE'; EXCEPTION WHEN duplicate_object THEN null; END;
END $$;
DO $$ BEGIN
    BEGIN ALTER TYPE public.tenant_plan ADD VALUE 'BASIC'; EXCEPTION WHEN duplicate_object THEN null; END;
END $$;
DO $$ BEGIN
    BEGIN ALTER TYPE public.tenant_plan ADD VALUE 'PREMIUM'; EXCEPTION WHEN duplicate_object THEN null; END;
END $$;
DO $$ BEGIN
    BEGIN ALTER TYPE public.tenant_plan ADD VALUE 'ENTERPRISE'; EXCEPTION WHEN duplicate_object THEN null; END;
END $$;
DO $$ BEGIN
    BEGIN ALTER TYPE public.user_role ADD VALUE 'SUPER_ADMIN'; EXCEPTION WHEN duplicate_object THEN null; END;
END $$;
DO $$ BEGIN
    BEGIN ALTER TYPE public.user_role ADD VALUE 'ADMIN'; EXCEPTION WHEN duplicate_object THEN null; END;
END $$;
DO $$ BEGIN
    BEGIN ALTER TYPE public.user_role ADD VALUE 'USER'; EXCEPTION WHEN duplicate_object THEN null; END;
END $$;
