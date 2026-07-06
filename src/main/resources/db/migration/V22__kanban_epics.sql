-- Epics (#321): kanban_item bekommt einen Typ (ITEM | EPIC) und eine optionale
-- Epic-Zuordnung (parent_id, selbstreferenzierender FK). Epics nehmen nicht am
-- Spalten-Workflow teil: sie erscheinen nicht auf dem Board und fallen aus dem
-- Positions-Namespace (siehe active_position unten).
--
-- Bestandsdaten: alle vorhandenen Zeilen sind normale Items -> type='ITEM',
-- parent_id=NULL (Spalten-Defaults, kein UPDATE noetig).

ALTER TABLE kanban_item
  ADD COLUMN type VARCHAR(20) NOT NULL DEFAULT 'ITEM' AFTER body,
  ADD COLUMN parent_id BIGINT NULL AFTER type;

-- Wird das Epic (physisch) geloescht, verlieren die Kinder nur die Zuordnung —
-- sie bleiben als eigenstaendige Items erhalten.
ALTER TABLE kanban_item
  ADD INDEX idx_kanban_item_parent (parent_id),
  ADD CONSTRAINT fk_kanban_item_parent
    FOREIGN KEY (parent_id) REFERENCES kanban_item (id) ON DELETE SET NULL;

-- Positions-Namespace (uk_kanban_active_position aus V19) um Epics bereinigen:
-- Epics halten keine aktive Position, sonst kollidierten sie mit den Items der
-- Spalte (der Reindex der Use-Cases zaehlt nur ITEMs). Virtuelle Spalte neu
-- definieren: archiviert ODER Epic -> NULL (faellt aus dem Unique-Index).
ALTER TABLE kanban_item DROP INDEX uk_kanban_active_position;
ALTER TABLE kanban_item DROP COLUMN active_position;
ALTER TABLE kanban_item
  ADD COLUMN active_position INT
  AS (IF(archived OR type = 'EPIC', NULL, position_in_column)) VIRTUAL;
ALTER TABLE kanban_item
  ADD CONSTRAINT uk_kanban_active_position
  UNIQUE (user_sub, column_name, active_position);
