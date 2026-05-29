-- Kanban-Kommentare (#120): freie Kommentare an einem Kanban-Item.
--
-- ON DELETE CASCADE: das Loeschen eines Items entfernt seine Kommentare automatisch,
-- so dass keine verwaisten Zeilen zurueckbleiben.
-- created_at/updated_at folgen der TIMESTAMP-Konvention aus V8 (kanban_item).

CREATE TABLE kanban_comment (
  id         BIGINT AUTO_INCREMENT PRIMARY KEY,
  item_id    BIGINT        NOT NULL,
  author     VARCHAR(255)  NOT NULL,
  body       TEXT          NOT NULL,
  created_at TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_kanban_comment_item
    FOREIGN KEY (item_id) REFERENCES kanban_item (id) ON DELETE CASCADE,
  INDEX idx_kanban_comment_item_created (item_id, created_at)
);
