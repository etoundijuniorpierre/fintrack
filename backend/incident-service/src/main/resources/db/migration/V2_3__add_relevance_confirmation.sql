-- Attente prolongee : la reprise par le traitant ne relance plus l'incident
-- directement, elle demande d'abord a l'entite source s'il est encore d'actualite.
-- Tant qu'une reponse est attendue, ces colonnes portent la demande en cours.
ALTER TABLE public.incidents
    ADD COLUMN IF NOT EXISTS confirmation_requested_at timestamp without time zone;

ALTER TABLE public.incidents
    ADD COLUMN IF NOT EXISTS confirmation_requested_by uuid;

ALTER TABLE public.incidents
    ADD COLUMN IF NOT EXISTS last_confirmation_reminder_sent_at timestamp without time zone;

-- Realigne la contrainte CHECK sur incident_histories.action avec l'enum ActionType,
-- qui gagne CONFIRMATION_REQUEST : la demande d'actualite ne change pas le statut,
-- elle a donc besoin de sa propre entree d'historique.
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
            'CONFIRMATION_REQUEST',
            'REOPENING',
            'RESOLUTION_REJECTED'
        )
    );
