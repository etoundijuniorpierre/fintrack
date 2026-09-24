-- =============================================================================
-- Script de validation de migration: incident_type_configs
-- Vérifie l'intégrité des données après migration de incident_db vers admin_db
-- =============================================================================

\echo '=== Validation de la migration incident_type_configs ==='

-- 1. Comparaison du nombre de lignes
\echo ''
\echo '--- 1. Comparaison du nombre de lignes ---'

WITH source_count AS (
    SELECT count AS cnt FROM dblink(
        'host=localhost port=5434 dbname=incidentservicedb user=postgres password=root',
        'SELECT COUNT(*) FROM incident_type_configs'
    ) AS t(count INTEGER)
),
target_count AS (
    SELECT COUNT(*) AS cnt FROM incident_type_configs
)
SELECT
    s.cnt AS source_rows,
    t.cnt AS target_rows,
    CASE WHEN s.cnt = t.cnt THEN 'OK' ELSE 'MISMATCH' END AS status
FROM source_count s, target_count t;

-- 2. Validation de la préservation des UUIDs
\echo ''
\echo '--- 2. UUIDs présents dans source mais absents de la cible ---'

SELECT source.id AS missing_uuid
FROM dblink(
    'host=localhost port=5434 dbname=incidentservicedb user=postgres password=root',
    'SELECT id FROM incident_type_configs'
) AS source(id UUID)
LEFT JOIN incident_type_configs target ON source.id = target.id
WHERE target.id IS NULL;

-- 3. Validation de l'intégrité des données (nom unique)
\echo ''
\echo '--- 3. Vérification des noms uniques dans la cible ---'

SELECT name, COUNT(*) AS occurrences
FROM incident_type_configs
GROUP BY name
HAVING COUNT(*) > 1;

-- 4. Vérification des données non nulles obligatoires
\echo ''
\echo '--- 4. Lignes avec données obligatoires manquantes ---'

SELECT id, name, display_name
FROM incident_type_configs
WHERE name IS NULL OR display_name IS NULL OR is_active IS NULL;

-- 5. Comparaison des données ligne par ligne (échantillon)
\echo ''
\echo '--- 5. Comparaison des données (vérification de cohérence) ---'

SELECT
    target.id,
    CASE WHEN target.name = source.name THEN 'OK' ELSE 'DIFF' END AS name_check,
    CASE WHEN target.display_name = source.display_name THEN 'OK' ELSE 'DIFF' END AS display_name_check,
    CASE WHEN target.is_active = source.is_active THEN 'OK' ELSE 'DIFF' END AS is_active_check,
    CASE WHEN target.sla_hours IS NOT DISTINCT FROM source.sla_hours THEN 'OK' ELSE 'DIFF' END AS sla_hours_check
FROM incident_type_configs target
JOIN dblink(
    'host=localhost port=5434 dbname=incidentservicedb user=postgres password=root',
    'SELECT id, name, display_name, is_active, sla_hours FROM incident_type_configs'
) AS source(id UUID, name VARCHAR, display_name VARCHAR, is_active BOOLEAN, sla_hours INTEGER)
ON target.id = source.id
WHERE target.name != source.name
   OR target.display_name != source.display_name
   OR target.is_active != source.is_active
   OR target.sla_hours IS DISTINCT FROM source.sla_hours;

\echo ''
\echo '=== Validation terminée ==='
