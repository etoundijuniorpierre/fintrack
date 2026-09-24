-- Les incidents critiques sont relances a intervalle regulier aupres du declarant, du
-- traitant et de leurs chefs : il faut savoir quand la derniere relance est partie pour
-- ne pas la rejouer a chaque passage du job.
ALTER TABLE public.incidents
    ADD COLUMN IF NOT EXISTS last_critical_reminder_sent_at timestamp without time zone;
