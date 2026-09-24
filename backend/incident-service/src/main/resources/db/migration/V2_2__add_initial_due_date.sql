-- Les metriques de conformite comparent la cloture a due_date. Or une reprise
-- (deblocage, confirmation apres attente prolongee) repousse due_date : le retard
-- accumule disparaissait des tableaux de bord. On fige l'echeance de reference,
-- posee a la qualification et jamais repoussee ensuite.
ALTER TABLE public.incidents
    ADD COLUMN IF NOT EXISTS initial_due_date timestamp without time zone;

-- Reprise de l'historique : pour les incidents deja crees, la meilleure approximation
-- de l'echeance de reference reste l'echeance courante.
UPDATE public.incidents
   SET initial_due_date = due_date
 WHERE initial_due_date IS NULL
   AND due_date IS NOT NULL;
