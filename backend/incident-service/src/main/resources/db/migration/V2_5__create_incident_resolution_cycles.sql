-- Un incident rouvert repartait a zero : resolved_at, closed_at et validated_at
-- etaient remis a NULL, effacant le cycle precedent. Les rapports d'un mois passe
-- changeaient donc retroactivement, et le delai de premiere reponse comparait le
-- validated_at du nouveau cycle au created_at d'origine.
-- Chaque cycle de traitement porte desormais ses propres jalons.
CREATE TABLE IF NOT EXISTS public.incident_resolution_cycles (
    id                uuid PRIMARY KEY,
    incident_id       uuid NOT NULL REFERENCES public.incidents (id),
    cycle_no          integer NOT NULL,
    started_at        timestamp without time zone NOT NULL,
    validated_at      timestamp without time zone,
    treated_at        timestamp without time zone,
    resolved_at       timestamp without time zone,
    closed_at         timestamp without time zone,
    due_date_snapshot timestamp without time zone,
    outcome           varchar(30) NOT NULL,
    paused_minutes    bigint NOT NULL DEFAULT 0,
    created_at        timestamp without time zone,
    updated_at        timestamp without time zone,
    modified_by       uuid,
    CONSTRAINT uk_incident_resolution_cycle_no UNIQUE (incident_id, cycle_no),
    CONSTRAINT incident_resolution_cycles_outcome_check CHECK (
        outcome IN ('OPEN', 'CLOSED', 'REJECTED', 'CANCELLED', 'REOPENED')
    )
);

CREATE INDEX IF NOT EXISTS idx_incident_cycle_closed_at
    ON public.incident_resolution_cycles (closed_at);

CREATE INDEX IF NOT EXISTS idx_incident_cycle_resolved_at
    ON public.incident_resolution_cycles (resolved_at);

-- Reprise de l'existant : un cycle unique par incident, reconstitue depuis les
-- colonnes scalaires. Les cycles anterieurs a une reouverture sont perdus (leurs
-- jalons avaient deja ete effaces), mais le cycle courant devient mesurable.
-- Idempotent : ne fait rien sur une base ou la reprise a deja tourne.
INSERT INTO public.incident_resolution_cycles (
    id, incident_id, cycle_no, started_at, validated_at, resolved_at, closed_at,
    due_date_snapshot, outcome, paused_minutes, created_at, updated_at
)
SELECT
    gen_random_uuid(),
    i.id,
    COALESCE(i.reopen_count, 0) + 1,
    COALESCE(i.reopened_at, i.created_at),
    i.validated_at,
    i.resolved_at,
    i.closed_at,
    i.initial_due_date,
    CASE i.status
        WHEN 'CLOSED' THEN 'CLOSED'
        WHEN 'REJECTED' THEN 'REJECTED'
        WHEN 'CANCELLED' THEN 'CANCELLED'
        ELSE 'OPEN'
    END,
    0,
    now(),
    now()
FROM public.incidents i
WHERE NOT EXISTS (
    SELECT 1 FROM public.incident_resolution_cycles c WHERE c.incident_id = i.id
);
