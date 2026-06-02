-- Duplikat-Erkennung fuer den Image-Store (#199): SHA-256-Hash der Binaerdaten als Hex-String
-- (64 Zeichen). Der Index beschleunigt die check-hash-Abfrage. Bestandsbilder bleiben mit NULL-Hash
-- (kein Backfill der Binaerdaten in der Migration); ihr Hash entsteht erst bei erneutem Upload.
ALTER TABLE stored_image ADD COLUMN sha256_hash VARCHAR(64) NULL;
CREATE INDEX idx_stored_image_hash ON stored_image (sha256_hash);
