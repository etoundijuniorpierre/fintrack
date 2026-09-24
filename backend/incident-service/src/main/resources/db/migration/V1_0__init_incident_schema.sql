-- Baseline incident-service : schema reel, aligne sur les entites JPA.
-- Genere depuis la base de reference. Ce service tourne en ddl-auto=update, mais la
-- baseline doit rester fidele : c'est elle qui cree le schema d'une base neuve.

CREATE TABLE IF NOT EXISTS public.incident_type_configs (
    id uuid NOT NULL,
    created_at timestamp(6) without time zone,
    modified_by uuid,
    updated_at timestamp(6) without time zone,
    closer_type character varying(50) DEFAULT 'ASSIGNEE'::character varying NOT NULL,
    default_target_service_id uuid,
    default_target_user_id uuid,
    description character varying(500),
    display_name character varying(200) NOT NULL,
    is_active boolean NOT NULL,
    name character varying(100) NOT NULL,
    requires_cause_analysis boolean DEFAULT false NOT NULL,
    requires_validation boolean DEFAULT true NOT NULL,
    sla_hours integer,
    validator_scope character varying(50) DEFAULT 'AGENCY_MANAGER'::character varying NOT NULL,
    default_criticality character varying(50),
    email_notifications_enabled boolean DEFAULT false NOT NULL,
    in_app_notifications_enabled boolean DEFAULT true NOT NULL,
    direction_validator_id uuid,
    requires_direction_validation boolean DEFAULT false NOT NULL,
    resolver_type character varying(50) DEFAULT 'SOURCE_AGENCY_MANAGER'::character varying NOT NULL,
    treater_type character varying(50) DEFAULT 'ASSIGNEE'::character varying NOT NULL,
    CONSTRAINT incident_type_configs_default_criticality_check CHECK (((default_criticality)::text = ANY ((ARRAY['LOW'::character varying, 'MEDIUM'::character varying, 'HIGH'::character varying, 'CRITICAL'::character varying])::text[]))),
    CONSTRAINT incident_type_configs_validator_scope_check CHECK (((validator_scope)::text = ANY ((ARRAY['AGENCY_MANAGER'::character varying, 'TARGET_SERVICE_MANAGER'::character varying])::text[])))
);

CREATE TABLE IF NOT EXISTS public.incident_type_config_direction_validators (
    incident_type_config_id uuid NOT NULL,
    validator_id uuid
);

CREATE TABLE IF NOT EXISTS public.incidents (
    id uuid NOT NULL,
    created_at timestamp(6) without time zone,
    modified_by uuid,
    updated_at timestamp(6) without time zone,
    agency_id uuid NOT NULL,
    assigned_to uuid,
    blocked_at timestamp(6) without time zone,
    blocked_by uuid,
    blocked_reason character varying(5000),
    cause character varying(100),
    cause_detail character varying(2000),
    closed_at timestamp(6) without time zone,
    created_by uuid NOT NULL,
    creator_service_id uuid,
    criticality character varying(50) NOT NULL,
    description character varying(5000) NOT NULL,
    due_date timestamp(6) without time zone,
    incident_date date,
    last_sla_reminder_sent_at timestamp(6) without time zone,
    observation_date date,
    reject_reason character varying(5000),
    reopen_count integer DEFAULT 0,
    reopen_reason character varying(5000),
    reopened_at timestamp(6) without time zone,
    reopened_by uuid,
    treatment_description character varying(5000),
    resolution_description character varying(5000),
    resolved_at timestamp(6) without time zone,
    source_incident_id uuid,
    status character varying(50) NOT NULL,
    title character varying(200) NOT NULL,
    transfer_reason character varying(5000),
    transferred_at timestamp(6) without time zone,
    transferred_to_service uuid,
    type_id uuid NOT NULL,
    unblocked_at timestamp(6) without time zone,
    validated_at timestamp(6) without time zone,
    validated_by uuid,
    closure_description character varying(5000),
    direction_rejection_reason character varying(10000),
    proposed_procedure character varying(15000),
    pre_block_status character varying(50),
    CONSTRAINT incidents_cause_check CHECK (((cause)::text = ANY ((ARRAY['HUMAN'::character varying, 'TECHNICAL'::character varying, 'ORGANIZATIONAL'::character varying, 'ENVIRONMENTAL'::character varying, 'EXTERNAL'::character varying, 'STRUCTURAL'::character varying, 'OTHER'::character varying])::text[]))),
    CONSTRAINT incidents_criticality_check CHECK (((criticality)::text = ANY ((ARRAY['LOW'::character varying, 'MEDIUM'::character varying, 'HIGH'::character varying, 'CRITICAL'::character varying])::text[])))
);

CREATE TABLE IF NOT EXISTS public.incident_histories (
    id uuid NOT NULL,
    created_at timestamp(6) without time zone,
    modified_by uuid,
    updated_at timestamp(6) without time zone,
    action character varying(100) NOT NULL,
    comment character varying(1000),
    new_value character varying(255),
    old_value character varying(255),
    user_id uuid,
    incident_id uuid NOT NULL
);

CREATE TABLE IF NOT EXISTS public.incident_comments (
    id uuid NOT NULL,
    created_at timestamp(6) without time zone,
    modified_by uuid,
    updated_at timestamp(6) without time zone,
    content character varying(2000) NOT NULL,
    is_internal boolean NOT NULL,
    user_id uuid NOT NULL,
    incident_id uuid NOT NULL,
    parent_comment_id uuid
);

-- Cles primaires, unicites et cles etrangeres. Les noms generes par Hibernate
-- sont conserves a l'identique pour rester comparable a la base de reference.
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'incident_type_configs_pkey') THEN
    ALTER TABLE public.incident_type_configs ADD CONSTRAINT incident_type_configs_pkey PRIMARY KEY (id);
  END IF;
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'incidents_pkey') THEN
    ALTER TABLE public.incidents ADD CONSTRAINT incidents_pkey PRIMARY KEY (id);
  END IF;
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'incident_histories_pkey') THEN
    ALTER TABLE public.incident_histories ADD CONSTRAINT incident_histories_pkey PRIMARY KEY (id);
  END IF;
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'incident_comments_pkey') THEN
    ALTER TABLE public.incident_comments ADD CONSTRAINT incident_comments_pkey PRIMARY KEY (id);
  END IF;
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ukiup4b7ospnlyjh9a0cnjly2l') THEN
    ALTER TABLE public.incident_type_configs ADD CONSTRAINT ukiup4b7ospnlyjh9a0cnjly2l UNIQUE (name);
  END IF;
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk9irjo0lp3obiasnuurnlxns9k') THEN
    ALTER TABLE public.incident_histories ADD CONSTRAINT fk9irjo0lp3obiasnuurnlxns9k FOREIGN KEY (incident_id) REFERENCES public.incidents(id);
  END IF;
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk7yn67aavek7o8dc4b3f5traga') THEN
    ALTER TABLE public.incident_comments ADD CONSTRAINT fk7yn67aavek7o8dc4b3f5traga FOREIGN KEY (incident_id) REFERENCES public.incidents(id);
  END IF;
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fkqn3r4vqc6sobq5lwhyomkpfpl') THEN
    ALTER TABLE public.incident_comments ADD CONSTRAINT fkqn3r4vqc6sobq5lwhyomkpfpl FOREIGN KEY (parent_comment_id) REFERENCES public.incident_comments(id);
  END IF;
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fkafuk9cl24oba54iywb0akm721') THEN
    ALTER TABLE public.incident_type_config_direction_validators ADD CONSTRAINT fkafuk9cl24oba54iywb0akm721 FOREIGN KEY (incident_type_config_id) REFERENCES public.incident_type_configs(id);
  END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_incident_history_action_created ON public.incident_histories USING btree (action, created_at);
CREATE INDEX IF NOT EXISTS idx_incident_history_action_new_created ON public.incident_histories USING btree (action, new_value, created_at);
CREATE INDEX IF NOT EXISTS idx_incident_history_incident_created ON public.incident_histories USING btree (incident_id, created_at);
CREATE INDEX IF NOT EXISTS idx_incident_history_user_action_created ON public.incident_histories USING btree (user_id, action, created_at);
CREATE INDEX IF NOT EXISTS idx_incidents_agency_created_at ON public.incidents USING btree (agency_id, created_at);
CREATE INDEX IF NOT EXISTS idx_incidents_assigned_status ON public.incidents USING btree (assigned_to, status);
CREATE INDEX IF NOT EXISTS idx_incidents_closed_at ON public.incidents USING btree (closed_at);
CREATE INDEX IF NOT EXISTS idx_incidents_created_by_created_at ON public.incidents USING btree (created_by, created_at);
CREATE INDEX IF NOT EXISTS idx_incidents_creator_service_created_at ON public.incidents USING btree (creator_service_id, created_at);
CREATE INDEX IF NOT EXISTS idx_incidents_due_status ON public.incidents USING btree (due_date, status);
CREATE INDEX IF NOT EXISTS idx_incidents_resolved_at ON public.incidents USING btree (resolved_at);
CREATE INDEX IF NOT EXISTS idx_incidents_status_created_at ON public.incidents USING btree (status, created_at);
CREATE INDEX IF NOT EXISTS idx_incidents_transferred_service_created_at ON public.incidents USING btree (transferred_to_service, created_at);
CREATE INDEX IF NOT EXISTS idx_incidents_validated_at ON public.incidents USING btree (validated_at);
