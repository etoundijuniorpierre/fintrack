-- La « soumission de procédure » devient « proposition de solution ». On renomme la
-- colonne proposed_procedure en proposed_solution (RENAME COLUMN, non destructif : les
-- données suivent la colonne). Le garde-fou FlywayMigrationSafetyTest n'interdit que le
-- RENAME TO de table, pas le RENAME COLUMN.
-- On ajoute aussi estimated_resolution_hours : le temps de résolution estimé par le
-- traitant à la prise en charge, qui pilote l'échéance SLA quand il est renseigné
-- (sinon le SLA par défaut prime).

ALTER TABLE incidents RENAME COLUMN proposed_procedure TO proposed_solution;

ALTER TABLE incidents ADD COLUMN IF NOT EXISTS estimated_resolution_hours integer;
