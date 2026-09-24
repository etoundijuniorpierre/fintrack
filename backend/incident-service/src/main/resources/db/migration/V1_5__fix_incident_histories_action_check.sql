-- Realigne la contrainte CHECK sur incident_histories.action avec l'enum ActionType.
-- Une ancienne contrainte (heritee d'un schema genere par Hibernate) n'incluait pas
-- les actions ajoutees depuis (REOPENING, RESOLUTION_REJECTED, TYPE_CHANGE) : toute
-- reouverture echouait a l'insertion de l'historique (REOPENING), la transaction etait
-- annulee et l'incident restait a RESOLU. On la remplace par la liste courante.
ALTER TABLE public.incident_histories
    DROP CONSTRAINT IF EXISTS incident_histories_action_check;

ALTER TABLE public.incident_histories
    ADD CONSTRAINT incident_histories_action_check CHECK (
        action IN (
            'CREATION',
            'VALIDATION',
            'TRANSFER',
            'STATUS_CHANGE',
            'COMMENT',
            'UPDATE',
            'ASSIGNMENT',
            'ROUTING',
            'TYPE_CHANGE',
            'REOPENING',
            'RESOLUTION_REJECTED'
        )
    );
