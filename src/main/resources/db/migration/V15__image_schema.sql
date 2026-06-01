-- Image-Store (#181): speichert hochgeladene Bilder als Binaerdaten. Das Dashboard-
-- Image-Widget (#179) referenziert spaeter nur die id, statt Base64 in der Widget-config
-- abzulegen. LONGBLOB fasst Bilder bis 5 MB problemlos (TEXT/config waere zu klein).

CREATE TABLE stored_image (
  id           BIGINT       NOT NULL PRIMARY KEY AUTO_INCREMENT,
  content_type VARCHAR(64)  NOT NULL,
  size_bytes   INT          NOT NULL,
  data         LONGBLOB     NOT NULL,
  created_at   TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;
