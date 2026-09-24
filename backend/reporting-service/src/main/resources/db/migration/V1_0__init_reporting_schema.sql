-- Baseline reporting-service : schema reel, aligne sur les entites JPA (ddl-auto=validate).
-- Genere depuis la base de reference ; toute divergence ici empeche le demarrage
-- du service sur une base neuve.

CREATE TABLE IF NOT EXISTS public.escalation_rule_events (
    id uuid NOT NULL,
    created_at timestamp(6) without time zone,
    modified_by uuid,
    updated_at timestamp(6) without time zone,
    details text,
    evaluated_at timestamp(6) without time zone NOT NULL,
    hit_count bigint NOT NULL,
    notifications_emitted bigint NOT NULL,
    outcome character varying(32) NOT NULL,
    rule_key character varying(64) NOT NULL
);

CREATE TABLE IF NOT EXISTS public.escalation_rules (
    id uuid NOT NULL,
    created_at timestamp(6) without time zone,
    modified_by uuid,
    updated_at timestamp(6) without time zone,
    action_label character varying(255) NOT NULL,
    automated boolean NOT NULL,
    enabled boolean NOT NULL,
    last_evaluation_count bigint,
    last_triggered_at timestamp(6) without time zone,
    notification_type character varying(64),
    owner_role character varying(64) NOT NULL,
    rule_key character varying(64) NOT NULL,
    trigger_label character varying(255) NOT NULL
);

CREATE TABLE IF NOT EXISTS public.generated_reports (
    id uuid NOT NULL,
    created_at timestamp(6) without time zone,
    modified_by uuid,
    updated_at timestamp(6) without time zone,
    agency_id uuid,
    auto_send_email boolean,
    created_by uuid NOT NULL,
    download_url character varying(255),
    email_recipients text,
    error_message text,
    file_path character varying(255),
    file_size bigint,
    filters text,
    format character varying(255) NOT NULL,
    generation_type character varying(255),
    metrics text,
    name character varying(255) NOT NULL,
    period_end timestamp(6) without time zone,
    period_start timestamp(6) without time zone,
    service_id uuid,
    status character varying(255) NOT NULL,
    type character varying(255) NOT NULL,
    content_type character varying(50),
    CONSTRAINT generated_reports_content_type_check CHECK (((content_type)::text = ANY ((ARRAY['OPERATIONAL'::character varying, 'INCIDENT_TYPE_ANALYSIS'::character varying])::text[]))),
    CONSTRAINT generated_reports_format_check CHECK (((format)::text = ANY ((ARRAY['PDF'::character varying, 'EXCEL'::character varying, 'JSON'::character varying])::text[]))),
    CONSTRAINT generated_reports_generation_type_check CHECK (((generation_type)::text = ANY ((ARRAY['MANUAL'::character varying, 'AUTOMATIC'::character varying])::text[]))),
    CONSTRAINT generated_reports_type_check CHECK (((type)::text = ANY ((ARRAY['DAILY'::character varying, 'WEEKLY'::character varying, 'MONTHLY'::character varying, 'CUSTOM'::character varying])::text[])))
);

CREATE TABLE IF NOT EXISTS public.report_schedules (
    id uuid NOT NULL,
    created_at timestamp(6) without time zone,
    modified_by uuid,
    updated_at timestamp(6) without time zone,
    created_by uuid NOT NULL,
    format character varying(50) NOT NULL,
    is_active boolean,
    last_generated_at timestamp(6) without time zone,
    name character varying(100) NOT NULL,
    recipient_emails text,
    scope character varying(50) NOT NULL,
    send_time time(0) without time zone NOT NULL,
    type character varying(50) NOT NULL,
    week_day integer,
    content_type character varying(50),
    CONSTRAINT report_schedules_content_type_check CHECK (((content_type)::text = ANY ((ARRAY['OPERATIONAL'::character varying, 'INCIDENT_TYPE_ANALYSIS'::character varying])::text[]))),
    CONSTRAINT report_schedules_format_check CHECK (((format)::text = ANY ((ARRAY['PDF'::character varying, 'EXCEL'::character varying, 'JSON'::character varying])::text[]))),
    CONSTRAINT report_schedules_type_check CHECK (((type)::text = ANY ((ARRAY['DAILY'::character varying, 'WEEKLY'::character varying, 'MONTHLY'::character varying, 'CUSTOM'::character varying])::text[])))
);

CREATE TABLE IF NOT EXISTS public.system_settings (
    id uuid NOT NULL,
    created_at timestamp(6) without time zone,
    modified_by uuid,
    updated_at timestamp(6) without time zone,
    category character varying(32) NOT NULL,
    description character varying(255),
    setting_key character varying(80) NOT NULL,
    setting_value text
);

DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'escalation_rule_events_pkey') THEN
    ALTER TABLE public.escalation_rule_events ADD CONSTRAINT escalation_rule_events_pkey PRIMARY KEY (id);
  END IF;
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'escalation_rules_pkey') THEN
    ALTER TABLE public.escalation_rules ADD CONSTRAINT escalation_rules_pkey PRIMARY KEY (id);
  END IF;
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'generated_reports_pkey') THEN
    ALTER TABLE public.generated_reports ADD CONSTRAINT generated_reports_pkey PRIMARY KEY (id);
  END IF;
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'report_schedules_pkey') THEN
    ALTER TABLE public.report_schedules ADD CONSTRAINT report_schedules_pkey PRIMARY KEY (id);
  END IF;
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'system_settings_pkey') THEN
    ALTER TABLE public.system_settings ADD CONSTRAINT system_settings_pkey PRIMARY KEY (id);
  END IF;
  -- Noms generes par Hibernate : conserves a l'identique pour rester comparable
  -- a la base de reference.
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uknm18l4pyovtvd8y3b3x0l2y64') THEN
    ALTER TABLE public.system_settings ADD CONSTRAINT uknm18l4pyovtvd8y3b3x0l2y64 UNIQUE (setting_key);
  END IF;
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ukt1a3frglh7jttffy1kf8sy2ds') THEN
    ALTER TABLE public.escalation_rules ADD CONSTRAINT ukt1a3frglh7jttffy1kf8sy2ds UNIQUE (rule_key);
  END IF;
END $$;
