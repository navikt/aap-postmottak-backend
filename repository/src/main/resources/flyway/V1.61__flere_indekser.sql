CREATE INDEX idx_journalpost_person_id on journalpost (person_id);

CREATE INDEX idx_regel_evaluering_resultat_id on regel_evaluering (regel_resultat_id);

CREATE INDEX idx_regelsett_resultat_innkommende_journalpost on regelsett_resultat (innkommende_journalpost);