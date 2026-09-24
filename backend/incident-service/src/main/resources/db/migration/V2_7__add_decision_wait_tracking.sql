-- Deux familles d'attente ont leur propre circuit de relance : une validation attendue
-- (le delai de traitement ne court pas), et une decision attendue apres traitement
-- (resolution, cloture). Il faut savoir depuis quand l'attente dure, et quand elle a
-- ete relancee.
ALTER TABLE public.incidents
    ADD COLUMN IF NOT EXISTS decision_awaited_since timestamp without time zone;

ALTER TABLE public.incidents
    ADD COLUMN IF NOT EXISTS last_decision_reminder_sent_at timestamp without time zone;

-- Reprise de l'historique : pour les incidents qui attendent deja une validation, la
-- meilleure approximation du debut de l'attente est leur date de creation. Les incidents
-- en attente de validation Direction sont laisses de cote : leur attente a commence a la
-- soumission de la solution, que rien ne date ici — leur premiere relance partira donc
-- apres la prochaine soumission.
UPDATE public.incidents
   SET decision_awaited_since = created_at
 WHERE decision_awaited_since IS NULL
   AND status = 'PENDING_VALIDATION';
