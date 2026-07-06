-- Positions-Integrität der Kanban-Spalten (#309): aktive Items sollen pro (user, column)
-- lückenlos und eindeutig positioniert sein. Archivierte Items behalten ihre alte
-- position_in_column, dürfen aber nicht mit aktiven kollidieren.

-- 1. Bestandsdaten normalisieren: aktive Items pro (user_sub, column_name) auf 0..n-1
--    umnummerieren, geordnet nach bisheriger Position (id als stabiler Tiebreaker bei
--    vorhandenen Duplikaten). Ohne diesen Schritt würde der Unique-Constraint an
--    bestehenden Kollisionen scheitern.
UPDATE kanban_item AS t
JOIN (
  SELECT id,
         ROW_NUMBER() OVER (
           PARTITION BY user_sub, column_name
           ORDER BY position_in_column, id
         ) - 1 AS rn
  FROM kanban_item
  WHERE archived = FALSE
) AS r ON t.id = r.id
SET t.position_in_column = r.rn
WHERE t.archived = FALSE;

-- 2. Generierte (virtuelle) Spalte: aktive Position. Archivierte Items -> NULL, damit sie
--    aus dem Unique-Namespace fallen (MariaDB erlaubt beliebig viele NULLs im Unique-Index).
ALTER TABLE kanban_item
  ADD COLUMN active_position INT
  AS (IF(archived, NULL, position_in_column)) VIRTUAL;

-- 3. Unique-Constraint auf die aktive Position pro (user_sub, column_name). Verhindert
--    doppelte Positionen aktiver Items — auch bei überlappenden parallelen Reindex-Läufen.
ALTER TABLE kanban_item
  ADD CONSTRAINT uk_kanban_active_position
  UNIQUE (user_sub, column_name, active_position);
