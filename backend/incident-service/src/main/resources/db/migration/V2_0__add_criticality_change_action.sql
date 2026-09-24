-- Realigne la contrainte CHECK sur incident_histories.action avec l'enum ActionType,
-- qui gagne CRITICALITY_CHANGE : la requalification d'un incident est desormais
-- historisee axe par axe (type / criticite) au lieu d'une entree UPDATE opaque.
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
            'CRITICALITY_CHANGE',
            'REOPENING',
            'RESOLUTION_REJECTED'
        )
    );
