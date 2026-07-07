-- Kanban-Listen-Filter serverseitig merken (#345). Die in der Listen-Ansicht aktiven
-- Filter-Keys (Spalten-Namen + 'archived') werden pro User als CSV gespeichert, damit die
-- Auswahl Bereichswechsel und Reload überlebt. NULL bei Bestandszeilen -> Default-Filter.

ALTER TABLE kanban_settings
  ADD COLUMN list_filters VARCHAR(255) NULL;
