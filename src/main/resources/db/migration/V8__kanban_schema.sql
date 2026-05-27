-- Kanban-Welle (#99): Items + per-User Settings.
--
-- column ist SQL-Keyword, daher Spalte column_name.
-- position ist auch reserviert (Window-Funktionen), daher position_in_column.

CREATE TABLE kanban_item (
  id                 BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_sub           VARCHAR(64)  NOT NULL,
  title              VARCHAR(200) NOT NULL,
  body               TEXT         NOT NULL,
  column_name        VARCHAR(20)  NOT NULL,
  position_in_column INT          NOT NULL,
  created_at         TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at         TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  moved_to_done_at   TIMESTAMP    NULL,
  INDEX idx_kanban_item_user_col_pos (user_sub, column_name, position_in_column),
  INDEX idx_kanban_item_done_cleanup (column_name, moved_to_done_at)
);

CREATE TABLE kanban_settings (
  user_sub            VARCHAR(64) PRIMARY KEY,
  done_retention_days INT         NOT NULL DEFAULT 5
);
