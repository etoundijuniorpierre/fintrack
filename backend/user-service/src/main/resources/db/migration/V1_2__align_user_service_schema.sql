-- Migration V1_2 : Complément d'alignement non-destructif du schéma Postgres avec les entités JPA (user-service)

-- 1. Table des permissions (au cas où non créée)
CREATE TABLE IF NOT EXISTS permissions (
    id          UUID PRIMARY KEY,
    name        VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(1000),
    created_at  TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 2. Colonnes manquantes dans roles
ALTER TABLE roles ADD COLUMN IF NOT EXISTS display_name VARCHAR(100);
ALTER TABLE roles ADD COLUMN IF NOT EXISTS is_system BOOLEAN NOT NULL DEFAULT FALSE;

-- 3. Table role_permissions.
CREATE TABLE IF NOT EXISTS role_permissions (
    role_id       UUID NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    permission_id UUID NOT NULL REFERENCES permissions(id) ON DELETE CASCADE,
    PRIMARY KEY (role_id, permission_id)
);

-- 4. Colonnes manquantes dans agencies
ALTER TABLE agencies ADD COLUMN IF NOT EXISTS city VARCHAR(100);
ALTER TABLE agencies ADD COLUMN IF NOT EXISTS address VARCHAR(1000);
ALTER TABLE agencies ADD COLUMN IF NOT EXISTS is_active BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE agencies ADD COLUMN IF NOT EXISTS head_user_id UUID;

-- 5. Colonnes manquantes dans services
ALTER TABLE services ADD COLUMN IF NOT EXISTS description VARCHAR(1000);
ALTER TABLE services ADD COLUMN IF NOT EXISTS is_active BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE services ADD COLUMN IF NOT EXISTS head_user_id UUID;

-- 6. Colonnes manquantes dans users
ALTER TABLE users ADD COLUMN IF NOT EXISTS phone_number BIGINT UNIQUE;
ALTER TABLE users ADD COLUMN IF NOT EXISTS password VARCHAR(255);
ALTER TABLE users ADD COLUMN IF NOT EXISTS is_active BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE users ADD COLUMN IF NOT EXISTS last_login TIMESTAMP WITHOUT TIME ZONE;
ALTER TABLE users ADD COLUMN IF NOT EXISTS is_first_login BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE users ADD COLUMN IF NOT EXISTS avatar_document_id UUID;
ALTER TABLE users ADD COLUMN IF NOT EXISTS temp_password VARCHAR(255);
ALTER TABLE users ADD COLUMN IF NOT EXISTS temp_password_created_at TIMESTAMP WITHOUT TIME ZONE;

-- 7. Tables d'association utilisateur <-> roles / permissions
CREATE TABLE IF NOT EXISTS user_roles (
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role_id UUID NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, role_id)
);

CREATE TABLE IF NOT EXISTS user_permissions (
    user_id       UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    permission_id UUID NOT NULL REFERENCES permissions(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, permission_id)
);

CREATE TABLE IF NOT EXISTS user_revoked_permissions (
    user_id       UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    permission_id UUID NOT NULL REFERENCES permissions(id) ON DELETE CASCADE,
    CONSTRAINT pk_user_revoked_permissions PRIMARY KEY (user_id, permission_id)
);

-- 8. Contraintes de clé étrangère pour head_user_id.
-- On teste la présence d'une FK sur la colonne, pas seulement celle de ce nom :
-- la baseline V1_0 pose déjà ces FK sous les noms générés par Hibernate, et
-- tester le nom seul les dupliquerait sur une base neuve.
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conrelid = 'agencies'::regclass AND contype = 'f'
          AND conkey = ARRAY[(SELECT attnum FROM pg_attribute
                              WHERE attrelid = 'agencies'::regclass AND attname = 'head_user_id')]
    ) THEN
        ALTER TABLE agencies ADD CONSTRAINT fk_agencies_head_user FOREIGN KEY (head_user_id) REFERENCES users(id) ON DELETE SET NULL;
    END IF;
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conrelid = 'services'::regclass AND contype = 'f'
          AND conkey = ARRAY[(SELECT attnum FROM pg_attribute
                              WHERE attrelid = 'services'::regclass AND attname = 'head_user_id')]
    ) THEN
        ALTER TABLE services ADD CONSTRAINT fk_services_head_user FOREIGN KEY (head_user_id) REFERENCES users(id) ON DELETE SET NULL;
    END IF;
END $$;
