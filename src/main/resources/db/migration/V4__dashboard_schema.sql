-- Dashboard-Phase-1-Schema (#39). Pro User mehrere Dashboards, pro Dashboard
-- mehrere Widgets. Owner-Filter laeuft serverseitig via JWT-sub, daher steht
-- user_sub als String-Column in dashboards (nicht in einer separaten User-Tabelle —
-- die Wahrheit ist Keycloak, wir spiegeln hier nur den sub).

CREATE TABLE dashboards (
  id          BIGINT       NOT NULL AUTO_INCREMENT,
  user_sub    VARCHAR(255) NOT NULL,
  name        VARCHAR(100) NOT NULL,
  is_default  BOOLEAN      NOT NULL DEFAULT FALSE,
  created_at  TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at  TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (id),
  INDEX idx_dashboards_user_sub (user_sub)
) ENGINE=InnoDB CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE TABLE widgets (
  id            BIGINT       NOT NULL AUTO_INCREMENT,
  dashboard_id  BIGINT       NOT NULL,
  type          VARCHAR(32)  NOT NULL,
  pos_x         INT          NOT NULL,
  pos_y         INT          NOT NULL,
  width         INT          NOT NULL,
  height        INT          NOT NULL,
  config        TEXT         NOT NULL,
  created_at    TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at    TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (id),
  INDEX idx_widgets_dashboard_id (dashboard_id),
  CONSTRAINT fk_widgets_dashboard
    FOREIGN KEY (dashboard_id) REFERENCES dashboards (id) ON DELETE CASCADE
) ENGINE=InnoDB CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
