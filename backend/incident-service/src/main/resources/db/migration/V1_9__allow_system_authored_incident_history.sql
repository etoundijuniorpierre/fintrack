-- Realigne incident_histories.user_id sur l'entite (nullable depuis b3fd573c).
-- Les bases creees par ddl-auto=update avant ce changement portent encore le NOT NULL
-- d'origine, que ni Hibernate (update ne relache jamais une contrainte) ni le baseline
-- V1_0 (jamais execute sur une base existante) ne retirent. Consequence : toute action
-- ecrite par le systeme (user_id NULL) echouait au commit -- notamment les transitions
-- automatiques SLA, qui restaient donc eternellement candidates sans jamais basculer.
ALTER TABLE public.incident_histories
    ALTER COLUMN user_id DROP NOT NULL;
