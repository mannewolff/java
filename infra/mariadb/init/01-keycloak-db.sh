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

# Root-Passwort NICHT als -p-Prozessargument (waere in `ps aux` im Container sichtbar, #314),
# sondern ueber die vom Client gelesene Umgebungsvariable MYSQL_PWD uebergeben.
export MYSQL_PWD="${MARIADB_ROOT_PASSWORD}"

# SQL-Bruch/Injection durch Sonderzeichen im Passwort verhindern (#314): einfache
# Anfuehrungszeichen fuers SQL-String-Literal verdoppeln. Bash setzt den Variablenwert im
# Heredoc nur einmal ein und expandiert ihn nicht erneut, daher koennen $/Backtick im Wert
# keinen Code ausfuehren; das ''-Escaping deckt den verbleibenden Bruch-Fall ('=Quote) ab.
KC_DB_PW_ESCAPED="${KEYCLOAK_DB_PASSWORD//"'"/"''"}"

mariadb -u root <<-EOSQL
  CREATE DATABASE IF NOT EXISTS keycloak
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;
  CREATE USER IF NOT EXISTS 'keycloak'@'%' IDENTIFIED BY '${KC_DB_PW_ESCAPED}';
  GRANT ALL PRIVILEGES ON keycloak.* TO 'keycloak'@'%';
  FLUSH PRIVILEGES;
EOSQL

unset MYSQL_PWD

echo "[keycloak-db-init] Schema 'keycloak' und User 'keycloak'@'%' bereit."
