-- Zeitreihen-Welle (#90): generischer Speicher fuer Time-Series-Daten
-- (Gewichtskurve, Fieberkurve, Sensor-Werte). DataType ist Metadatum fuer
-- Anzeige und Validierung — gespeichert wird immer als DECIMAL(20,6).
--
-- Hinweis zur Flyway-Nummer: V5/V6/V7 wurden in der Issue-Vorlage genannt,
-- sind aber durch V8 (Kanban) ueberholt. Flyway verbietet Luecken in der
-- Versionsfolge, daher V9.

CREATE TABLE time_series (
  id          BIGINT       AUTO_INCREMENT PRIMARY KEY,
  user_sub    VARCHAR(64)  NOT NULL,
  name        VARCHAR(200) NOT NULL,
  description VARCHAR(500),
  unit        VARCHAR(50)  NOT NULL,
  data_type   VARCHAR(20)  NOT NULL,
  created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_time_series_user (user_sub)
);

CREATE TABLE time_series_entry (
  id              BIGINT         AUTO_INCREMENT PRIMARY KEY,
  time_series_id  BIGINT         NOT NULL,
  timestamp_value TIMESTAMP(6)   NOT NULL,
  numeric_value   DECIMAL(20, 6) NOT NULL,
  CONSTRAINT fk_time_series_entry_series
    FOREIGN KEY (time_series_id) REFERENCES time_series (id) ON DELETE CASCADE,
  INDEX idx_time_series_entry_series_ts (time_series_id, timestamp_value)
);
