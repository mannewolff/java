-- Fortlaufende, pro User eindeutige Anzeige-Nummer für Kanban-Items (#187).
-- Bestehende Items werden je User in id-Reihenfolge mit 1..n nummeriert. Der Unique-Index
-- erzwingt Eindeutigkeit pro User (inkl. archivierter Items, damit Nummern nie kollidieren).

ALTER TABLE kanban_item ADD COLUMN number INT NOT NULL DEFAULT 0;

UPDATE kanban_item AS t
JOIN (
  SELECT id, ROW_NUMBER() OVER (PARTITION BY user_sub ORDER BY id) AS rn
  FROM kanban_item
) AS r ON t.id = r.id
SET t.number = r.rn;

CREATE UNIQUE INDEX uk_kanban_item_number_per_user ON kanban_item (user_sub, number);
