-- V2_7 n'a repris que les attentes de VALIDATION. Les attentes d'ACTION apres
-- traitement (TRAITE, RESOLU) sont restees sans date de depart : leur circuit de
-- relance ne les voyait donc pas, et le stock existant n'aurait ete relance qu'a
-- la faveur d'une prochaine transition.
-- RESOLU porte sa propre date ; TRAITE n'en a aucune (le jalon vit dans les cycles
-- depuis V2_5, pas sur l'incident) : sa derniere ecriture en est la meilleure
-- approximation.
UPDATE public.incidents
   SET decision_awaited_since = COALESCE(resolved_at, updated_at, created_at)
 WHERE decision_awaited_since IS NULL
   AND status = 'RESOLVED';

UPDATE public.incidents
   SET decision_awaited_since = COALESCE(updated_at, created_at)
 WHERE decision_awaited_since IS NULL
   AND status = 'TREATED';
