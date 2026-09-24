-- Baseline document-service : schema reel, aligne sur les entites JPA (ddl-auto=validate).
-- Genere depuis la base de reference ; toute divergence ici empeche le demarrage
-- du service sur une base neuve.

CREATE TABLE IF NOT EXISTS public.attachments_metadata (
    id uuid NOT NULL,
    created_at timestamp(6) without time zone,
    modified_by uuid,
    updated_at timestamp(6) without time zone,
    category character varying(50),
    file_size bigint NOT NULL,
    filename character varying(255) NOT NULL,
    incident_id uuid,
    mime_type character varying(100),
    storage_path character varying(500) NOT NULL,
    uploaded_at timestamp(6) without time zone NOT NULL,
    uploaded_by uuid NOT NULL,
    comment_id uuid
);

DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'attachments_metadata_pkey') THEN
    ALTER TABLE public.attachments_metadata ADD CONSTRAINT attachments_metadata_pkey PRIMARY KEY (id);
  END IF;
END $$;
