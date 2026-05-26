#!/bin/bash
# Init-Script fuer MariaDB. Wird genau einmal beim ersten Start des Containers
# ausgefuehrt (wenn das mariadb-data-Volume noch leer ist) und legt das
# Keycloak-Schema sowie den dedizierten Keycloak-DB-User an.
#
# Bei bestehendem Volume hat dieses Script keinen Effekt — siehe infra/keycloak/README.md
# fuer die manuelle Variante.
set -euo pipefail

if [[ -z "${KEYCLOAK_DB_PASSWORD:-}" ]]; then
  echo "[keycloak-db-init] KEYCLOAK_DB_PASSWORD nicht gesetzt — Keycloak-DB wird NICHT angelegt." >&2
  exit 0
fi

mariadb -u root -p"${MARIADB_ROOT_PASSWORD}" <<-EOSQL
  CREATE DATABASE IF NOT EXISTS keycloak
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;
  CREATE USER IF NOT EXISTS 'keycloak'@'%' IDENTIFIED BY '${KEYCLOAK_DB_PASSWORD}';
  GRANT ALL PRIVILEGES ON keycloak.* TO 'keycloak'@'%';
  FLUSH PRIVILEGES;
EOSQL

echo "[keycloak-db-init] Schema 'keycloak' und User 'keycloak'@'%' bereit."
