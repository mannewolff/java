-- Sicherheit/Korrektheit (#317.4): Kommentar-Eigentum stabil an den Keycloak-`sub`
-- binden statt an den `preferred_username`. Der Username kann in Keycloak umbenannt
-- werden; danach schlaegt die bisherige Ownership-Pruefung (author == preferred_username)
-- fuer alle Alt-Kommentare fehl (403), obwohl es dieselbe Person ist. `sub` ist stabil.
--
-- `author` bleibt als Anzeigename erhalten. Neu: `author_sub` traegt die Identitaet.
--
-- Backfill: Ein Kommentar haengt an genau einem kanban_item; kommentiert wird nur das
-- eigene Item (Owner-Schutz in den Use-Cases). Der Autor eines Alt-Kommentars ist daher
-- der Eigentuemer des Items -> author_sub aus kanban_item.user_sub uebernehmen.

ALTER TABLE kanban_comment
  ADD COLUMN author_sub VARCHAR(64) NULL AFTER author;

UPDATE kanban_comment c
  JOIN kanban_item i ON c.item_id = i.id
  SET c.author_sub = i.user_sub
  WHERE c.author_sub IS NULL;

ALTER TABLE kanban_comment
  MODIFY author_sub VARCHAR(64) NOT NULL;
