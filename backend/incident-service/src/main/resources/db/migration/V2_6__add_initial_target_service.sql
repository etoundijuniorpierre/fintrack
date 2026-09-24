-- Le taux de transfert doit distinguer le routage initial d'un reroutage vers un
-- autre service. transferred_to_service est ecrase a chaque transfert : sans point
-- de depart fige, la distinction est impossible. Meme role que initial_due_date
-- face a due_date.
ALTER TABLE public.incidents
    ADD COLUMN IF NOT EXISTS initial_target_service_id uuid;

-- Reprise : exact pour tout incident jamais transfere. Pour ceux deja transferes,
-- le service cible a la creation est irrecuperable (l'historique ne conservait que
-- les statuts) : ils apparaitront comme non reroutes, le taux est donc sous-estime
-- sur l'anterieur a cette migration.
UPDATE public.incidents
SET initial_target_service_id = transferred_to_service
WHERE initial_target_service_id IS NULL;
