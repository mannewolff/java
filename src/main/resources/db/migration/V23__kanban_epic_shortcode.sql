-- Epic-Kürzel (#329): optionales, frei wählbares Label eines Epics. Nur an Epics
-- befüllt; ohne Kürzel leitet das Frontend eins aus den Titel-Initialen ab.
-- Bestandsdaten -> NULL (Fallback greift).

ALTER TABLE kanban_item
  ADD COLUMN shortcode VARCHAR(16) NULL AFTER parent_id;
