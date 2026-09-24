-- Realigne attachments_metadata.incident_id sur l'entite (nullable).
-- Toutes les pieces jointes ne sont pas rattachees a un incident : l'avatar
-- utilisateur (categorie USER_AVATAR) est stocke avec incident_id null. Un NOT NULL
-- herite du schema d'origine ferait echouer l'envoi d'avatar.
ALTER TABLE public.attachments_metadata
    ALTER COLUMN incident_id DROP NOT NULL;
