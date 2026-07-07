-- Datei-Anhänge für Kanban-Einträge (#349). Beliebige Dateien werden als Blob pro Eintrag
-- gespeichert; ON DELETE CASCADE räumt Anhänge beim Löschen des Items/Epics automatisch weg.
-- item_id referenziert kanban_item — deckt Items UND Epics ab (beide liegen in dieser Tabelle).

CREATE TABLE kanban_attachment (
  id              BIGINT       NOT NULL PRIMARY KEY AUTO_INCREMENT,
  item_id         BIGINT       NOT NULL,
  filename        VARCHAR(255) NOT NULL,
  content_type    VARCHAR(128) NOT NULL,
  size_bytes      INT          NOT NULL,
  data            LONGBLOB     NOT NULL,
  sha256_hash     VARCHAR(64),
  uploaded_by_sub VARCHAR(64)  NOT NULL,
  created_at      DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  CONSTRAINT fk_kanban_attachment_item
    FOREIGN KEY (item_id) REFERENCES kanban_item (id) ON DELETE CASCADE,
  INDEX idx_kanban_attachment_item (item_id, created_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;
