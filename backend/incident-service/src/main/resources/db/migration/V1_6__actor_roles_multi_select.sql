-- Etapes du workflow configurables en MULTI-choix : traitement, resolution, cloture
-- et reouverture admettent chacune plusieurs roles habilites (SOURCE_AGENCY_MANAGER,
-- CREATOR, ASSIGNEE, CHEF_SERVICE). Remplace les colonnes mono-valeur closer/resolver/
-- treater_type par des tables de jointure (@ElementCollection) et introduit la
-- reouverture. Le role composite 'BOTH' devient {CREATOR, ASSIGNEE}.

CREATE TABLE IF NOT EXISTS public.incident_type_config_treater_roles (
    incident_type_config_id uuid NOT NULL
        REFERENCES public.incident_type_configs(id) ON DELETE CASCADE,
    role varchar(50) NOT NULL
);
CREATE TABLE IF NOT EXISTS public.incident_type_config_resolver_roles (
    incident_type_config_id uuid NOT NULL
        REFERENCES public.incident_type_configs(id) ON DELETE CASCADE,
    role varchar(50) NOT NULL
);
CREATE TABLE IF NOT EXISTS public.incident_type_config_closer_roles (
    incident_type_config_id uuid NOT NULL
        REFERENCES public.incident_type_configs(id) ON DELETE CASCADE,
    role varchar(50) NOT NULL
);
CREATE TABLE IF NOT EXISTS public.incident_type_config_reopener_roles (
    incident_type_config_id uuid NOT NULL
        REFERENCES public.incident_type_configs(id) ON DELETE CASCADE,
    role varchar(50) NOT NULL
);

-- Backfill depuis les colonnes mono-valeur existantes.
INSERT INTO public.incident_type_config_treater_roles (incident_type_config_id, role)
SELECT id, treater_type FROM public.incident_type_configs WHERE treater_type IS NOT NULL;

INSERT INTO public.incident_type_config_resolver_roles (incident_type_config_id, role)
SELECT id, resolver_type FROM public.incident_type_configs WHERE resolver_type IS NOT NULL;

-- Cloture : 'BOTH' historique (createur OU assigne) => deux roles distincts.
INSERT INTO public.incident_type_config_closer_roles (incident_type_config_id, role)
SELECT id, closer_type FROM public.incident_type_configs
WHERE closer_type IS NOT NULL AND closer_type <> 'BOTH';
INSERT INTO public.incident_type_config_closer_roles (incident_type_config_id, role)
SELECT id, 'CREATOR' FROM public.incident_type_configs WHERE closer_type = 'BOTH';
INSERT INTO public.incident_type_config_closer_roles (incident_type_config_id, role)
SELECT id, 'ASSIGNEE' FROM public.incident_type_configs WHERE closer_type = 'BOTH';

-- Reouverture : nouvelle etape, defaut = chef d'agence source (exclut le traitant).
INSERT INTO public.incident_type_config_reopener_roles (incident_type_config_id, role)
SELECT id, 'SOURCE_AGENCY_MANAGER' FROM public.incident_type_configs;

-- Les anciennes colonnes closer_type / resolver_type / treater_type ne sont plus
-- mappees par l'entite (leur DEFAULT NOT NULL d'origine reste renseigne cote base).
-- On ne les supprime PAS : les migrations doivent rester additives (cf.
-- FlywayMigrationSafetyTest) pour garantir un deploiement sans perte de donnees.
-- Elles pourront etre retirees plus tard par une operation de maintenance dediee.
