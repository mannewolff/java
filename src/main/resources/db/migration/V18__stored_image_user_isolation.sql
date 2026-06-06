-- Image-Store user-isolieren (#230). Bisher war stored_image (V15) ohne Eigentümer-Kontext:
-- jeder ROLE_USER konnte alle Bilder listen, per Hash pruefen, laden und loeschen — inkonsistent
-- zu dashboard/kanban/timeseries (konsequent owner-scoped). Entscheidung beim Rollout: bestehende
-- Bilder LOESCHEN (clean slate), damit die NOT-NULL-Spalte ohne Backfill eingefuehrt werden kann.

TRUNCATE TABLE stored_image;

ALTER TABLE stored_image
  ADD COLUMN user_sub VARCHAR(64) NOT NULL AFTER id;

CREATE INDEX idx_stored_image_user_sub ON stored_image (user_sub);
