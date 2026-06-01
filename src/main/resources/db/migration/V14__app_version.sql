-- App-Versionsverwaltung (#158): genau eine Zeile (id = 1) haelt die aktuelle
-- Major.Minor-Version. Startwert 0.1. Wird ueber UseCases/Endpoint erhoeht.
-- CHECK (id = 1) erzwingt, dass nur die eine Versionszeile existieren kann.

CREATE TABLE app_version (
  id         BIGINT    NOT NULL PRIMARY KEY,
  major      INT       NOT NULL,
  minor      INT       NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT chk_app_version_single_row CHECK (id = 1)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

INSERT INTO app_version (id, major, minor) VALUES (1, 0, 1);
