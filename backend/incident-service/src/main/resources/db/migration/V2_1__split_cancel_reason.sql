-- Le motif d'annulation partageait la colonne reject_reason avec le motif de rejet :
-- deux decisions distinctes, deux acteurs distincts, un seul nom de colonne qui mentait
-- sur la moitie de ses usages. On lui donne sa propre colonne.
ALTER TABLE public.incidents
    ADD COLUMN IF NOT EXISTS cancel_reason character varying(5000);

-- Reprise de l'historique : les incidents deja annules portent leur motif dans
-- reject_reason. On le recopie, puis on solde reject_reason sur ces lignes seulement
-- (la valeur reste presente, dans sa colonne legitime).
UPDATE public.incidents
   SET cancel_reason = reject_reason
 WHERE status = 'CANCELLED'
   AND reject_reason IS NOT NULL
   AND cancel_reason IS NULL;

UPDATE public.incidents
   SET reject_reason = NULL
 WHERE status = 'CANCELLED'
   AND cancel_reason IS NOT NULL;
