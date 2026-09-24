-- Realigne users.password sur l'entite (nullable depuis 68d8dfc2 relache).
-- Toute creation de compte passe par setupTemporaryPassword() : le mot de passe
-- definitif est null tant que l'utilisateur n'a pas fait son premier login, seul
-- temp_password est renseigne. Un NOT NULL herite du schema d'origine ferait
-- echouer la creation d'utilisateur.
ALTER TABLE public.users
    ALTER COLUMN password DROP NOT NULL;
