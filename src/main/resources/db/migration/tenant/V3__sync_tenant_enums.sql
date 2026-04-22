-- Generated Scope: TENANT
DO $$ BEGIN
    BEGIN ALTER TYPE addon_type ADD VALUE 'DAILY'; EXCEPTION WHEN duplicate_object THEN null; END;
END $$;
DO $$ BEGIN
    BEGIN ALTER TYPE addon_type ADD VALUE 'ONE_TIME'; EXCEPTION WHEN duplicate_object THEN null; END;
END $$;
DO $$ BEGIN
    BEGIN ALTER TYPE audit_action ADD VALUE 'CREATE'; EXCEPTION WHEN duplicate_object THEN null; END;
END $$;
DO $$ BEGIN
    BEGIN ALTER TYPE audit_action ADD VALUE 'UPDATE'; EXCEPTION WHEN duplicate_object THEN null; END;
END $$;
DO $$ BEGIN
    BEGIN ALTER TYPE audit_action ADD VALUE 'DELETE'; EXCEPTION WHEN duplicate_object THEN null; END;
END $$;
DO $$ BEGIN
    BEGIN ALTER TYPE audit_action ADD VALUE 'LOGIN'; EXCEPTION WHEN duplicate_object THEN null; END;
END $$;
DO $$ BEGIN
    BEGIN ALTER TYPE audit_action ADD VALUE 'LOGOUT'; EXCEPTION WHEN duplicate_object THEN null; END;
END $$;
DO $$ BEGIN
    BEGIN ALTER TYPE booking_status ADD VALUE 'PENDING'; EXCEPTION WHEN duplicate_object THEN null; END;
END $$;
DO $$ BEGIN
    BEGIN ALTER TYPE booking_status ADD VALUE 'CONFIRMED'; EXCEPTION WHEN duplicate_object THEN null; END;
END $$;
DO $$ BEGIN
    BEGIN ALTER TYPE booking_status ADD VALUE 'ACTIVE'; EXCEPTION WHEN duplicate_object THEN null; END;
END $$;
DO $$ BEGIN
    BEGIN ALTER TYPE booking_status ADD VALUE 'COMPLETED'; EXCEPTION WHEN duplicate_object THEN null; END;
END $$;
DO $$ BEGIN
    BEGIN ALTER TYPE booking_status ADD VALUE 'CANCELLED'; EXCEPTION WHEN duplicate_object THEN null; END;
END $$;
DO $$ BEGIN
    BEGIN ALTER TYPE chat_role ADD VALUE 'USER'; EXCEPTION WHEN duplicate_object THEN null; END;
END $$;
DO $$ BEGIN
    BEGIN ALTER TYPE chat_role ADD VALUE 'ASSISTANT'; EXCEPTION WHEN duplicate_object THEN null; END;
END $$;
DO $$ BEGIN
    BEGIN ALTER TYPE chat_role ADD VALUE 'SYSTEM'; EXCEPTION WHEN duplicate_object THEN null; END;
END $$;
DO $$ BEGIN
    BEGIN ALTER TYPE discount_type ADD VALUE 'PERCENTAGE'; EXCEPTION WHEN duplicate_object THEN null; END;
END $$;
DO $$ BEGIN
    BEGIN ALTER TYPE discount_type ADD VALUE 'FIXED'; EXCEPTION WHEN duplicate_object THEN null; END;
END $$;
DO $$ BEGIN
    BEGIN ALTER TYPE fuel_type ADD VALUE 'PETROL'; EXCEPTION WHEN duplicate_object THEN null; END;
END $$;
DO $$ BEGIN
    BEGIN ALTER TYPE fuel_type ADD VALUE 'DIESEL'; EXCEPTION WHEN duplicate_object THEN null; END;
END $$;
DO $$ BEGIN
    BEGIN ALTER TYPE fuel_type ADD VALUE 'ELECTRIC'; EXCEPTION WHEN duplicate_object THEN null; END;
END $$;
DO $$ BEGIN
    BEGIN ALTER TYPE fuel_type ADD VALUE 'HYBRID'; EXCEPTION WHEN duplicate_object THEN null; END;
END $$;
DO $$ BEGIN
    BEGIN ALTER TYPE maintenance_type ADD VALUE 'OIL_CHANGE'; EXCEPTION WHEN duplicate_object THEN null; END;
END $$;
DO $$ BEGIN
    BEGIN ALTER TYPE maintenance_type ADD VALUE 'TIRE'; EXCEPTION WHEN duplicate_object THEN null; END;
END $$;
DO $$ BEGIN
    BEGIN ALTER TYPE maintenance_type ADD VALUE 'INSPECTION'; EXCEPTION WHEN duplicate_object THEN null; END;
END $$;
DO $$ BEGIN
    BEGIN ALTER TYPE maintenance_type ADD VALUE 'REPAIR'; EXCEPTION WHEN duplicate_object THEN null; END;
END $$;
DO $$ BEGIN
    BEGIN ALTER TYPE notification_channel ADD VALUE 'EMAIL'; EXCEPTION WHEN duplicate_object THEN null; END;
END $$;
DO $$ BEGIN
    BEGIN ALTER TYPE notification_channel ADD VALUE 'PUSH'; EXCEPTION WHEN duplicate_object THEN null; END;
END $$;
DO $$ BEGIN
    BEGIN ALTER TYPE notification_channel ADD VALUE 'SMS'; EXCEPTION WHEN duplicate_object THEN null; END;
END $$;
DO $$ BEGIN
    BEGIN ALTER TYPE payment_method ADD VALUE 'CARD'; EXCEPTION WHEN duplicate_object THEN null; END;
END $$;
DO $$ BEGIN
    BEGIN ALTER TYPE payment_method ADD VALUE 'CASH'; EXCEPTION WHEN duplicate_object THEN null; END;
END $$;
DO $$ BEGIN
    BEGIN ALTER TYPE payment_status ADD VALUE 'PENDING'; EXCEPTION WHEN duplicate_object THEN null; END;
END $$;
DO $$ BEGIN
    BEGIN ALTER TYPE payment_status ADD VALUE 'PAID'; EXCEPTION WHEN duplicate_object THEN null; END;
END $$;
DO $$ BEGIN
    BEGIN ALTER TYPE payment_status ADD VALUE 'FAILED'; EXCEPTION WHEN duplicate_object THEN null; END;
END $$;
DO $$ BEGIN
    BEGIN ALTER TYPE payment_status ADD VALUE 'REFUNDED'; EXCEPTION WHEN duplicate_object THEN null; END;
END $$;
DO $$ BEGIN
    BEGIN ALTER TYPE ticket_priority ADD VALUE 'LOW'; EXCEPTION WHEN duplicate_object THEN null; END;
END $$;
DO $$ BEGIN
    BEGIN ALTER TYPE ticket_priority ADD VALUE 'NORMAL'; EXCEPTION WHEN duplicate_object THEN null; END;
END $$;
DO $$ BEGIN
    BEGIN ALTER TYPE ticket_priority ADD VALUE 'HIGH'; EXCEPTION WHEN duplicate_object THEN null; END;
END $$;
DO $$ BEGIN
    BEGIN ALTER TYPE ticket_priority ADD VALUE 'URGENT'; EXCEPTION WHEN duplicate_object THEN null; END;
END $$;
DO $$ BEGIN
    BEGIN ALTER TYPE ticket_status ADD VALUE 'OPEN'; EXCEPTION WHEN duplicate_object THEN null; END;
END $$;
DO $$ BEGIN
    BEGIN ALTER TYPE ticket_status ADD VALUE 'IN_PROGRESS'; EXCEPTION WHEN duplicate_object THEN null; END;
END $$;
DO $$ BEGIN
    BEGIN ALTER TYPE ticket_status ADD VALUE 'RESOLVED'; EXCEPTION WHEN duplicate_object THEN null; END;
END $$;
DO $$ BEGIN
    BEGIN ALTER TYPE ticket_status ADD VALUE 'CLOSED'; EXCEPTION WHEN duplicate_object THEN null; END;
END $$;
DO $$ BEGIN
    BEGIN ALTER TYPE transmission_type ADD VALUE 'MANUAL'; EXCEPTION WHEN duplicate_object THEN null; END;
END $$;
DO $$ BEGIN
    BEGIN ALTER TYPE transmission_type ADD VALUE 'AUTOMATIC'; EXCEPTION WHEN duplicate_object THEN null; END;
END $$;
DO $$ BEGIN
    BEGIN ALTER TYPE vehicle_status ADD VALUE 'AVAILABLE'; EXCEPTION WHEN duplicate_object THEN null; END;
END $$;
DO $$ BEGIN
    BEGIN ALTER TYPE vehicle_status ADD VALUE 'RENTED'; EXCEPTION WHEN duplicate_object THEN null; END;
END $$;
DO $$ BEGIN
    BEGIN ALTER TYPE vehicle_status ADD VALUE 'MAINTENANCE'; EXCEPTION WHEN duplicate_object THEN null; END;
END $$;
DO $$ BEGIN
    BEGIN ALTER TYPE vehicle_status ADD VALUE 'INACTIVE'; EXCEPTION WHEN duplicate_object THEN null; END;
END $$;
