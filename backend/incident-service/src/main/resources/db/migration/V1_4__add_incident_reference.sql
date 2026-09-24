-- Reference lisible des incidents (FT-I-2026-0001) et son compteur annuel.
-- Purement additif : aucune donnee existante n'est lue ni modifiee. Le rattrapage
-- des incidents anterieurs est fait par l'application au demarrage.
ALTER TABLE incidents ADD COLUMN IF NOT EXISTS reference VARCHAR(20);

CREATE TABLE IF NOT EXISTS incident_reference_sequences (
    reference_year INTEGER PRIMARY KEY,
    last_number BIGINT NOT NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_incidents_reference ON incidents(reference);
