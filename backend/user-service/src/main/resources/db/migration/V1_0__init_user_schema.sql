-- Baseline user-service : schema reel, aligne sur les entites JPA (ddl-auto=validate).
-- Genere depuis la base de reference ; toute divergence ici empeche le demarrage
-- du service sur une base neuve.

CREATE TABLE IF NOT EXISTS public.agencies (
    id uuid NOT NULL,
    created_at timestamp(6) without time zone,
    modified_by uuid,
    updated_at timestamp(6) without time zone,
    address character varying(1000),
    city character varying(100),
    code character varying(100) NOT NULL,
    is_active boolean NOT NULL,
    name character varying(100) NOT NULL,
    head_user_id uuid
);

CREATE TABLE IF NOT EXISTS public.permissions (
    id uuid NOT NULL,
    created_at timestamp(6) without time zone,
    modified_by uuid,
    updated_at timestamp(6) without time zone,
    description character varying(1000),
    name character varying(100) NOT NULL
);

CREATE TABLE IF NOT EXISTS public.roles (
    id uuid NOT NULL,
    created_at timestamp(6) without time zone,
    modified_by uuid,
    updated_at timestamp(6) without time zone,
    description character varying(1000),
    display_name character varying(100),
    is_system boolean NOT NULL,
    name character varying(100) NOT NULL
);

CREATE TABLE IF NOT EXISTS public.services (
    id uuid NOT NULL,
    created_at timestamp(6) without time zone,
    modified_by uuid,
    updated_at timestamp(6) without time zone,
    description character varying(1000),
    is_active boolean NOT NULL,
    name character varying(100) NOT NULL,
    head_user_id uuid
);

CREATE TABLE IF NOT EXISTS public.users (
    id uuid NOT NULL,
    created_at timestamp(6) without time zone,
    modified_by uuid,
    updated_at timestamp(6) without time zone,
    avatar_document_id uuid,
    email character varying(100),
    failed_login_attempts integer NOT NULL,
    first_name character varying(100),
    is_active boolean NOT NULL,
    is_first_login boolean NOT NULL,
    last_login timestamp(6) without time zone,
    last_name character varying(100),
    password character varying(255),
    phone_number bigint,
    temp_password character varying(255),
    username character varying(100) NOT NULL,
    agency_id uuid,
    service_id uuid,
    temp_password_created_at timestamp(6) without time zone
);

CREATE TABLE IF NOT EXISTS public.role_permissions (
    role_id uuid NOT NULL,
    permission_id uuid NOT NULL
);

CREATE TABLE IF NOT EXISTS public.user_managed_services (
    user_id uuid NOT NULL,
    service_id uuid NOT NULL
);

CREATE TABLE IF NOT EXISTS public.user_permissions (
    user_id uuid NOT NULL,
    permission_id uuid NOT NULL
);

CREATE TABLE IF NOT EXISTS public.user_revoked_permissions (
    user_id uuid NOT NULL,
    permission_id uuid NOT NULL
);

CREATE TABLE IF NOT EXISTS public.user_roles (
    user_id uuid NOT NULL,
    role_id uuid NOT NULL
);

-- Cles primaires, unicites et cles etrangeres. Les noms generes par Hibernate
-- sont conserves a l'identique pour rester comparable a la base de reference.
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'agencies_pkey') THEN
    ALTER TABLE public.agencies ADD CONSTRAINT agencies_pkey PRIMARY KEY (id);
  END IF;
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'permissions_pkey') THEN
    ALTER TABLE public.permissions ADD CONSTRAINT permissions_pkey PRIMARY KEY (id);
  END IF;
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'roles_pkey') THEN
    ALTER TABLE public.roles ADD CONSTRAINT roles_pkey PRIMARY KEY (id);
  END IF;
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'services_pkey') THEN
    ALTER TABLE public.services ADD CONSTRAINT services_pkey PRIMARY KEY (id);
  END IF;
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'users_pkey') THEN
    ALTER TABLE public.users ADD CONSTRAINT users_pkey PRIMARY KEY (id);
  END IF;
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'role_permissions_pkey') THEN
    ALTER TABLE public.role_permissions ADD CONSTRAINT role_permissions_pkey PRIMARY KEY (role_id, permission_id);
  END IF;
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'user_managed_services_pkey') THEN
    ALTER TABLE public.user_managed_services ADD CONSTRAINT user_managed_services_pkey PRIMARY KEY (user_id, service_id);
  END IF;
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'user_permissions_pkey') THEN
    ALTER TABLE public.user_permissions ADD CONSTRAINT user_permissions_pkey PRIMARY KEY (user_id, permission_id);
  END IF;
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'user_revoked_permissions_pkey') THEN
    ALTER TABLE public.user_revoked_permissions ADD CONSTRAINT user_revoked_permissions_pkey PRIMARY KEY (user_id, permission_id);
  END IF;
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'user_roles_pkey') THEN
    ALTER TABLE public.user_roles ADD CONSTRAINT user_roles_pkey PRIMARY KEY (user_id, role_id);
  END IF;

  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uk508sdjiaidsenrgq0secbysus') THEN
    ALTER TABLE public.agencies ADD CONSTRAINT uk508sdjiaidsenrgq0secbysus UNIQUE (name);
  END IF;
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ukog25hrdt7u2ots7k3c0lva021') THEN
    ALTER TABLE public.agencies ADD CONSTRAINT ukog25hrdt7u2ots7k3c0lva021 UNIQUE (code);
  END IF;
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ukrepkr38bakqibt9b45cn5lkx9') THEN
    ALTER TABLE public.agencies ADD CONSTRAINT ukrepkr38bakqibt9b45cn5lkx9 UNIQUE (head_user_id);
  END IF;
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ukh4rqgjwnqidx6mvj4i22dxwxe') THEN
    ALTER TABLE public.services ADD CONSTRAINT ukh4rqgjwnqidx6mvj4i22dxwxe UNIQUE (name);
  END IF;
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ukofx66keruapi6vyqpv6f2or37') THEN
    ALTER TABLE public.roles ADD CONSTRAINT ukofx66keruapi6vyqpv6f2or37 UNIQUE (name);
  END IF;
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ukpnvtwliis6p05pn6i3ndjrqt2') THEN
    ALTER TABLE public.permissions ADD CONSTRAINT ukpnvtwliis6p05pn6i3ndjrqt2 UNIQUE (name);
  END IF;
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ukr43af9ap4edm43mmtq01oddj6') THEN
    ALTER TABLE public.users ADD CONSTRAINT ukr43af9ap4edm43mmtq01oddj6 UNIQUE (username);
  END IF;
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uk6dotkott2kjsp8vw4d0m25fb7') THEN
    ALTER TABLE public.users ADD CONSTRAINT uk6dotkott2kjsp8vw4d0m25fb7 UNIQUE (email);
  END IF;
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uk9q63snka3mdh91as4io72espi') THEN
    ALTER TABLE public.users ADD CONSTRAINT uk9q63snka3mdh91as4io72espi UNIQUE (phone_number);
  END IF;

  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk7bo7u275rpljcaj3paqbaxbp9') THEN
    ALTER TABLE public.users ADD CONSTRAINT fk7bo7u275rpljcaj3paqbaxbp9 FOREIGN KEY (agency_id) REFERENCES public.agencies(id);
  END IF;
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fkg28emhyfqgy7bu8nv5ol805wt') THEN
    ALTER TABLE public.users ADD CONSTRAINT fkg28emhyfqgy7bu8nv5ol805wt FOREIGN KEY (service_id) REFERENCES public.services(id);
  END IF;
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fkg8vsq1hb3jt2fk7e5s6a5n6f5') THEN
    ALTER TABLE public.agencies ADD CONSTRAINT fkg8vsq1hb3jt2fk7e5s6a5n6f5 FOREIGN KEY (head_user_id) REFERENCES public.users(id);
  END IF;
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fkj1e23ordar0t7x0ht93q3jja8') THEN
    ALTER TABLE public.services ADD CONSTRAINT fkj1e23ordar0t7x0ht93q3jja8 FOREIGN KEY (head_user_id) REFERENCES public.users(id);
  END IF;
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fkn5fotdgk8d1xvo8nav9uv3muc') THEN
    ALTER TABLE public.role_permissions ADD CONSTRAINT fkn5fotdgk8d1xvo8nav9uv3muc FOREIGN KEY (role_id) REFERENCES public.roles(id);
  END IF;
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fkegdk29eiy7mdtefy5c7eirr6e') THEN
    ALTER TABLE public.role_permissions ADD CONSTRAINT fkegdk29eiy7mdtefy5c7eirr6e FOREIGN KEY (permission_id) REFERENCES public.permissions(id);
  END IF;
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fkhfh9dx7w3ubf1co1vdev94g3f') THEN
    ALTER TABLE public.user_roles ADD CONSTRAINT fkhfh9dx7w3ubf1co1vdev94g3f FOREIGN KEY (user_id) REFERENCES public.users(id);
  END IF;
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fkh8ciramu9cc9q3qcqiv4ue8a6') THEN
    ALTER TABLE public.user_roles ADD CONSTRAINT fkh8ciramu9cc9q3qcqiv4ue8a6 FOREIGN KEY (role_id) REFERENCES public.roles(id);
  END IF;
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fkkowxl8b2bngrxd1gafh13005u') THEN
    ALTER TABLE public.user_permissions ADD CONSTRAINT fkkowxl8b2bngrxd1gafh13005u FOREIGN KEY (user_id) REFERENCES public.users(id);
  END IF;
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fkq4qlrabt4s0etm9tfkoqfuib1') THEN
    ALTER TABLE public.user_permissions ADD CONSTRAINT fkq4qlrabt4s0etm9tfkoqfuib1 FOREIGN KEY (permission_id) REFERENCES public.permissions(id);
  END IF;
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk2lri9da00rv8ydjae1j6buxf6') THEN
    ALTER TABLE public.user_revoked_permissions ADD CONSTRAINT fk2lri9da00rv8ydjae1j6buxf6 FOREIGN KEY (user_id) REFERENCES public.users(id);
  END IF;
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk2pa69795xb3rje2n48b9tsady') THEN
    ALTER TABLE public.user_revoked_permissions ADD CONSTRAINT fk2pa69795xb3rje2n48b9tsady FOREIGN KEY (permission_id) REFERENCES public.permissions(id);
  END IF;
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk6lmyj65g4qc86jv5lisvrewsj') THEN
    ALTER TABLE public.user_managed_services ADD CONSTRAINT fk6lmyj65g4qc86jv5lisvrewsj FOREIGN KEY (user_id) REFERENCES public.users(id);
  END IF;
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fkech1wo3u00m2j8pqbbth5i24o') THEN
    ALTER TABLE public.user_managed_services ADD CONSTRAINT fkech1wo3u00m2j8pqbbth5i24o FOREIGN KEY (service_id) REFERENCES public.services(id);
  END IF;
END $$;

-- Regle metier portee par la base : un seul SUPER_ADMIN dans le systeme.
CREATE OR REPLACE FUNCTION public.check_single_super_admin() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
    DECLARE
        role_name VARCHAR;
        super_admin_count INT;
    BEGIN
        SELECT name INTO role_name FROM roles WHERE id = NEW.role_id;
        IF role_name = 'SUPER_ADMIN' THEN
            SELECT COUNT(*) INTO super_admin_count
            FROM user_roles ur
            JOIN roles r ON ur.role_id = r.id
            WHERE r.name = 'SUPER_ADMIN';

            IF super_admin_count > 0 THEN
                RAISE EXCEPTION 'Violation de règle métier DB : Il ne peut y avoir qu''un seul SUPER_ADMIN dans le système.';
            END IF;
        END IF;
        RETURN NEW;
    END;
    $$;

DROP TRIGGER IF EXISTS trg_enforce_single_super_admin ON public.user_roles;
CREATE TRIGGER trg_enforce_single_super_admin
    BEFORE INSERT ON public.user_roles
    FOR EACH ROW EXECUTE FUNCTION public.check_single_super_admin();
