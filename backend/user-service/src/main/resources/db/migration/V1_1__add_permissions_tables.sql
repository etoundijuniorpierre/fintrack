-- Tables manquantes pour le modele de permissions granulaires et les associations
-- utilisateurs <-> roles / permissions.

-- Table principale : permissions unitaires du systeme.
CREATE TABLE IF NOT EXISTS permissions (
    id         UUID PRIMARY KEY,
    name       VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(1000),
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Association many-to-many : utilisateurs <-> roles.
CREATE TABLE IF NOT EXISTS user_roles (
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role_id UUID NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, role_id)
);

-- Permissions accordees directement a un utilisateur (en plus de celles du role).
CREATE TABLE IF NOT EXISTS user_permissions (
    user_id       UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    permission_id UUID NOT NULL REFERENCES permissions(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, permission_id)
);

-- Permissions explicitement retirees a un utilisateur (soustraites de l'union
-- directes + role lors du calcul des permissions effectives).
CREATE TABLE IF NOT EXISTS user_revoked_permissions (
    user_id       UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    permission_id UUID NOT NULL REFERENCES permissions(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, permission_id)
);
