-- Autorise le nouveau contenu de rapport "situation par statut"
-- (INCIDENT_STATUS_OVERVIEW) sur les rapports generes et les planifications.
-- Les contraintes CHECK du baseline V1_0 ne listaient que OPERATIONAL et
-- INCIDENT_TYPE_ANALYSIS : on les remplace pour inclure la nouvelle valeur.

ALTER TABLE public.generated_reports
    DROP CONSTRAINT IF EXISTS generated_reports_content_type_check;
ALTER TABLE public.generated_reports
    ADD CONSTRAINT generated_reports_content_type_check
    CHECK (((content_type)::text = ANY ((ARRAY[
        'OPERATIONAL'::character varying,
        'INCIDENT_TYPE_ANALYSIS'::character varying,
        'INCIDENT_STATUS_OVERVIEW'::character varying
    ])::text[])));

ALTER TABLE public.report_schedules
    DROP CONSTRAINT IF EXISTS report_schedules_content_type_check;
ALTER TABLE public.report_schedules
    ADD CONSTRAINT report_schedules_content_type_check
    CHECK (((content_type)::text = ANY ((ARRAY[
        'OPERATIONAL'::character varying,
        'INCIDENT_TYPE_ANALYSIS'::character varying,
        'INCIDENT_STATUS_OVERVIEW'::character varying
    ])::text[])));
