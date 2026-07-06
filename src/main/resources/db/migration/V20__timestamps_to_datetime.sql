-- Datenintegritaet (#317.1): Alle Zeitstempel-Spalten von TIMESTAMP auf DATETIME
-- umstellen. MariaDB-TIMESTAMP deckt nur 1970-01-01..2038-01-19 ab; ein vom Client
-- gelieferter Zeitstempel ausserhalb dieses Fensters (Zukunftswert, historischer
-- CSV-Import vor 1970) wird von Bean-Validation/Domain akzeptiert, aber von der DB
-- abgelehnt -> unbehandelte DataIntegrityViolationException -> HTTP 500.
--
-- DATETIME(6) deckt 1000-01-01..9999-12-31 mit Mikrosekunden ab und beseitigt den
-- Bereich als Fehlerquelle. Die praezise Variante (6) bleibt dort erhalten, wo sie
-- schon galt (V4/V9/V15); die uebrigen Spalten behalten Sekundengenauigkeit.
--
-- Reine Typ-Verbreiterung: Defaults (CURRENT_TIMESTAMP) und ON-UPDATE-Verhalten
-- bleiben identisch, nur der zugrunde liegende Typ waechst.

-- V4: dashboards + widgets (Mikrosekunden)
ALTER TABLE dashboards
  MODIFY created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  MODIFY updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6);

ALTER TABLE widgets
  MODIFY created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  MODIFY updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6);

-- V8: kanban_item
ALTER TABLE kanban_item
  MODIFY created_at       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  MODIFY updated_at       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  MODIFY moved_to_done_at DATETIME NULL;

-- V9: time_series (Metadaten) + time_series_entry (client-gelieferter Messzeitpunkt)
ALTER TABLE time_series
  MODIFY created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  MODIFY updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;

ALTER TABLE time_series_entry
  MODIFY timestamp_value DATETIME(6) NOT NULL;

-- V10: ingest_token
ALTER TABLE ingest_token
  MODIFY created_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  MODIFY last_used_at DATETIME NULL;

-- V11: kanban_comment
ALTER TABLE kanban_comment
  MODIFY created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  MODIFY updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;

-- V14: app_version
ALTER TABLE app_version
  MODIFY created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  MODIFY updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;

-- V15: stored_image (Mikrosekunden)
ALTER TABLE stored_image
  MODIFY created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6);
