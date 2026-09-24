-- Flyway Migration V1_1 : 3-Step Workflow Roles and Pre-Block Status Restoration

ALTER TABLE incident_type_configs ADD COLUMN IF NOT EXISTS resolver_type VARCHAR(255) DEFAULT 'SOURCE_AGENCY_MANAGER';
ALTER TABLE incident_type_configs ADD COLUMN IF NOT EXISTS treater_type VARCHAR(255) DEFAULT 'ASSIGNEE';
ALTER TABLE incident_type_configs ADD COLUMN IF NOT EXISTS closer_type VARCHAR(255) DEFAULT 'ASSIGNEE';
ALTER TABLE incidents ADD COLUMN IF NOT EXISTS pre_block_status VARCHAR(255);

-- Initialisation des lignes existantes si la valeur est NULL
UPDATE incident_type_configs SET resolver_type = 'SOURCE_AGENCY_MANAGER' WHERE resolver_type IS NULL;
UPDATE incident_type_configs SET treater_type = 'ASSIGNEE' WHERE treater_type IS NULL;
UPDATE incident_type_configs SET closer_type = 'ASSIGNEE' WHERE closer_type IS NULL;

-- Suppression dynamique des contraintes CHECK générées par Hibernate sur les énumérations
DO $$
DECLARE
    r RECORD;
BEGIN
    FOR r IN (
        SELECT con.conname
        FROM pg_catalog.pg_constraint con
        INNER JOIN pg_catalog.pg_class rel ON rel.oid = con.conrelid
        INNER JOIN pg_catalog.pg_attribute att ON att.attrelid = rel.oid AND att.attnum = ANY(con.conkey)
        WHERE rel.relname = 'incident_type_configs'
          AND att.attname IN ('closer_type', 'resolver_type', 'treater_type')
          AND con.contype = 'c'
    ) LOOP
        EXECUTE 'ALTER TABLE incident_type_configs DROP CONSTRAINT IF EXISTS ' || quote_ident(r.conname);
    END LOOP;
END $$;
