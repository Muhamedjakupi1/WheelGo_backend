ALTER TABLE users
DROP CONSTRAINT users_tenant_id_fkey,
ADD CONSTRAINT users_tenant_id_fkey
FOREIGN KEY (tenant_id)
REFERENCES tenants(id)
ON DELETE CASCADE;