-- Pro Dashboard konfigurierbare Hintergrundfarbe (#129). NULL = Theme-Default
-- (background.default im Frontend). CSS-Farbwert als String (#1a1a2e, rgba(...)).

ALTER TABLE dashboards
  ADD COLUMN background_color VARCHAR(64) NULL AFTER name;
