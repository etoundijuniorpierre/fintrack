-- Etend le scope de validation : ajoute SOURCE_SERVICE_MANAGER (nouveau defaut) et ADMIN.
-- Additif : recreation de la contrainte CHECK avec le sur-ensemble de valeurs, nouveau
-- defaut de colonne. Aucune ligne existante n'est modifiee (ajustement manuel par type).

ALTER TABLE incident_type_configs
  DROP CONSTRAINT IF EXISTS incident_type_configs_validator_scope_check;

ALTER TABLE incident_type_configs
  ADD CONSTRAINT incident_type_configs_validator_scope_check
  CHECK (validator_scope IN (
    'SOURCE_SERVICE_MANAGER',
    'AGENCY_MANAGER',
    'TARGET_SERVICE_MANAGER',
    'ADMIN'
  ));

ALTER TABLE incident_type_configs
  ALTER COLUMN validator_scope SET DEFAULT 'SOURCE_SERVICE_MANAGER';
