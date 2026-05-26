-- Historischer Stand. Wollte die books-Tabelle entfernen, traf aber den falschen
-- Namen (V1 erstellte 'book' (Singular), hier wird 'books' (Plural) gedroppt).
-- Die echte Bereinigung passiert in V3__cleanup_book_remnants.sql idempotent.
-- Datei bleibt im Repo, weil sie in flyway_schema_history der laufenden DB steht.
DROP TABLE IF EXISTS books;
