-- Abhängigkeiten zwischen Kanban-Einträgen (#352). Kommagetrennte Liste der Board-Anzeige-
-- Nummern (nicht IDs), von denen ein Eintrag abhängt, z. B. "12,34". NULL/leer = keine.
-- Existenz- und Selbstreferenz-Prüfung erfolgt im Use-Case; hier nur die Spalte.

ALTER TABLE kanban_item
  ADD COLUMN dependencies VARCHAR(255) NULL AFTER shortcode;
