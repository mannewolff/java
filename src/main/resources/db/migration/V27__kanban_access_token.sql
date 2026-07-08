-- Board-Anbindung (#363): Kanban-Access-Token (PAT) fuer den 9-Schritt-Workflow.
-- Langlebiger, widerrufbarer Token analog zum Ingest-Token (V10). Plaintext
-- (tk_<64-hex>) wird nur einmal ausgeliefert; in der DB liegt ausschliesslich der
-- SHA-256-Hash. display_name traegt den Autor-Namen fuer token-erzeugte Kommentare.
-- DATETIME statt TIMESTAMP (post-V20-Konvention: kein 2038-Limit).

CREATE TABLE kanban_access_token (
  id            BIGINT       AUTO_INCREMENT PRIMARY KEY,
  user_sub      VARCHAR(64)  NOT NULL,
  display_name  VARCHAR(255) NOT NULL,
  name          VARCHAR(100) NOT NULL,
  token_hash    VARCHAR(100) NOT NULL UNIQUE,
  created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  last_used_at  DATETIME     NULL,
  revoked       BOOLEAN      NOT NULL DEFAULT FALSE,
  INDEX idx_kanban_access_token_user (user_sub),
  INDEX idx_kanban_access_token_hash_active (token_hash, revoked)
);
