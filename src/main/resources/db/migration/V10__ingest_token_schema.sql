-- Zeitreihen-Welle (#92): Ingest-Token-Speicher fuer die oeffentliche
-- POST-Schnittstelle. Plaintext-Token (tk_<64-hex>) wird nur einmal
-- ausgeliefert; in der DB liegt ausschliesslich der bcrypt-Hash.
--
-- Hinweis zur Flyway-Nummer: Issue-Vorlage nannte V6, durch V8/V9 aber
-- ueberholt. V10 ist der erste freie Slot.

CREATE TABLE ingest_token (
  id            BIGINT       AUTO_INCREMENT PRIMARY KEY,
  user_sub      VARCHAR(64)  NOT NULL,
  name          VARCHAR(100) NOT NULL,
  token_hash    VARCHAR(100) NOT NULL UNIQUE,
  created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  last_used_at  TIMESTAMP    NULL,
  revoked       BOOLEAN      NOT NULL DEFAULT FALSE,
  INDEX idx_ingest_token_user (user_sub),
  INDEX idx_ingest_token_hash_active (token_hash, revoked)
);
