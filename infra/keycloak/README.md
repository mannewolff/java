# Keycloak — Realm-Konfiguration als Code

Dieses Verzeichnis enthält die deklarative Konfiguration der beiden Keycloak-Realms
für die Toolbox-Anwendung. Die JSON-Dateien werden beim ersten Container-Start
durch das `--import-realm`-Flag automatisch importiert.

## Realms

| Datei | Realm | Zweck | Redirect-URIs |
|---|---|---|---|
| `realm-toolbox-dev.json` | `toolbox-dev` | Lokale Entwicklung | `http://localhost:5173/*`, `http://localhost:8080/*` |
| `realm-toolbox.json` | `toolbox` | Vorlage für Produktion | `https://toolbox.mwolff.org/*` |

Beide Realms sind strukturell identisch. Sie definieren:

- Drei Realm-Rollen: `PENDING` (Default für Selbstregistrierungen), `USER`, `ADMIN`.
- Zwei Clients: `toolbox-web` (public, PKCE) und `toolbox-api` (bearer-only).
- Self-Registration ist aktiviert, neue User landen in der Rolle `PENDING`.
  Ein Admin schaltet sie zu `USER` frei — siehe [Admin-Approval-Workflow](#admin-approval-workflow).
- Passwort-Policy: mindestens 12 Zeichen, mindestens ein Sonderzeichen, kein Username im Passwort.
- TOTP ist optional pro User (nicht erzwungen).
- Bruteforce-Schutz ist aktiviert.

Die Files sind **nicht-sensibel**: sie enthalten weder User-Daten noch Client-Secrets
noch das Keycloak-Master-Realm. Sie können bedenkenlos eingecheckt werden.

## Erst-Start (Dev)

```bash
# 1. .env aus .env.example kopieren und Passwoerter setzen
cp .env.example .env

# 2. Stack starten — beim ersten Start wird das Schema 'keycloak' angelegt
#    und beide Realms importiert.
docker-compose up -d

# 3. Keycloak-Admin-UI: http://localhost:8081/  (Login: KEYCLOAK_ADMIN aus .env)
# 4. Login-Seite des dev-Realms:
#    http://localhost:8081/realms/toolbox-dev/account/
```

## Bestehende Installation: Schema manuell anlegen

Die Initialisierung des `keycloak`-Schemas läuft nur beim ersten Start des MariaDB-
Containers (leeres Volume). Wenn das Volume `mariadb-data` bereits existiert, das
Schema fehlt aber, manuell anlegen:

```bash
docker compose exec mariadb mariadb -u root -p"$DB_ROOT_PASSWORD" <<'SQL'
  CREATE DATABASE IF NOT EXISTS keycloak
    CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
  CREATE USER IF NOT EXISTS 'keycloak'@'%' IDENTIFIED BY 'CHANGE_ME';
  GRANT ALL PRIVILEGES ON keycloak.* TO 'keycloak'@'%';
  FLUSH PRIVILEGES;
SQL
```

Der Wert `CHANGE_ME` muss mit dem in der `.env` gesetzten `KEYCLOAK_DB_PASSWORD`
übereinstimmen.

## Realm-Export / Aktualisierung

Wenn ein Realm in der laufenden Keycloak-Instanz manuell geändert wurde und der
Stand in dieses Repo zurückwandern soll:

```bash
# Innerhalb des Containers exportieren — beachtet die im Issue verlangte
# Strippung von User-Daten und Secrets:
docker compose exec keycloak \
  /opt/keycloak/bin/kc.sh export \
  --file /tmp/realm-toolbox-dev.json \
  --realm toolbox-dev \
  --users skip

docker compose cp keycloak:/tmp/realm-toolbox-dev.json \
  ./infra/keycloak/realm-toolbox-dev.json
```

Vor dem Commit das resultierende JSON auf Secrets prüfen (z. B. `clientSecret`,
`smtpServer.password`, Crypto-Keys). Diese Felder gehören **nicht** ins Repo.

## Admin-Approval-Workflow

Keycloak bietet out-of-the-box keinen „User wartet auf Freischaltung"-Status.
Wir bilden den Workflow über die Default-Rolle `PENDING` ab:

1. User registriert sich selbst auf `/realms/<name>/account/`.
2. Keycloak weist ihm automatisch die Realm-Rolle `PENDING` zu.
3. Spring-Backend lehnt API-Aufrufe für `PENDING`-User ab (keine `USER`-Rolle
   im `realm_access.roles`-Claim). Siehe Issue #37.
4. Ein Admin entfernt im Keycloak-UI manuell `PENDING` und setzt `USER`.
5. Beim nächsten Token-Refresh kann der User die Anwendung benutzen.

Wenn später ein automatisierter Approval-Flow (z. B. via E-Mail an Admin) gewünscht
ist, dafür ein eigenes Issue anlegen.

## Prod-Setup (Hostinger / eigener Server)

### Voraussetzungen (extern)

1. DNS A/AAAA-Record `auth.toolbox.mwolff.org` zeigt auf den Server.
2. Reverse-Proxy (Caddy / Nginx / Traefik / Hostinger-Built-in) leitet
   `https://auth.toolbox.mwolff.org:443` auf `127.0.0.1:8081` weiter und setzt
   `X-Forwarded-Proto`, `X-Forwarded-Host`, `X-Forwarded-For`.
3. TLS-Zertifikat für die Subdomain ist aktiv.

### Schritte auf dem Server

```bash
# 1. Override-File aus der versionierten Vorlage erzeugen
cp docker-compose.prod.yml.example docker-compose.override.yml

# 2. .env ergaenzen (drei neue Variablen)
cat <<'ENV' >> .env
KEYCLOAK_ADMIN=admin
KEYCLOAK_ADMIN_PASSWORD=<langer-Wert>
KEYCLOAK_DB_PASSWORD=<eigener-Wert>
ENV

# 3. Keycloak-Schema in der bestehenden MariaDB anlegen.
#    Auf Prod ist das mariadb-data-Volume NICHT leer (echte App-Daten),
#    daher laeuft das Init-Script aus infra/mariadb/init/ NICHT —
#    Schema einmalig manuell anlegen. mariadb-Container muss laufen.
docker compose up -d mariadb
docker compose exec mariadb mariadb -u root -p"$(grep '^DB_ROOT_PASSWORD=' .env | cut -d= -f2-)" <<SQL
CREATE DATABASE IF NOT EXISTS keycloak
  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER IF NOT EXISTS 'keycloak'@'%' IDENTIFIED BY '$(grep '^KEYCLOAK_DB_PASSWORD=' .env | cut -d= -f2-)';
GRANT ALL PRIVILEGES ON keycloak.* TO 'keycloak'@'%';
FLUSH PRIVILEGES;
SQL

# 4. Stack komplett hochfahren (lokale Builds, remote Pulls)
docker compose pull
docker compose up -d --build

# 5. Realm-Import + Boot abwarten
docker compose logs -f keycloak | grep -iE "imported|started in"
```

Anschließend in der Keycloak-UI sofort das Admin-Passwort rotieren und
`KEYCLOAK_ADMIN_PASSWORD` aus `.env` entfernen (Bootstrap-Variablen werden
nach dem ersten Start ignoriert).

### Stolperfallen

- **Niemals `--optimized` in der Override-Datei.** Das offizielle Image ist
  DB-neutral; mit `--optimized` weigert sich Keycloak zu starten, weil der
  Augmentation-Build für `KC_DB=mariadb` noch nicht passiert ist.
- **Doppelte Port-Mappings.** Compose merged Port-Listen statt sie zu
  ersetzen. Die Vorlage überschreibt deshalb keine Ports — das Default-Mapping
  `127.0.0.1:8081:8080` aus `docker-compose.yml` bleibt aktiv.
  Wenn du den Port doch komplett ersetzen willst (z.B. anderen Host-Port),
  brauchst du `ports: !override [...]` und Compose ≥ v2.24.
- **DB-Verbindung schlägt fehl.** Wenn Keycloak `Unable to connect to
  database` loggt, ist meist das `keycloak`-Schema noch nicht angelegt
  oder das Passwort in `.env` weicht von dem im SQL ab.

## Sicherheit

- Die Bootstrap-Credentials (`KEYCLOAK_ADMIN`, `KEYCLOAK_ADMIN_PASSWORD`) sind
  nur für den allerersten Start gedacht. In jeder ernsthaften Umgebung muss
  der Admin-User sofort danach via Keycloak-UI auf ein langes Passwort gesetzt
  und idealerweise mit TOTP versehen werden.
- `KEYCLOAK_DB_PASSWORD` ist ein separates Passwort vom Datenbank-API-User.
  Es darf nicht in das Repo, sondern nur in die lokale `.env`.
- `docker-compose.yml` exponiert Keycloak nur auf `127.0.0.1:8081`. Der
  Reverse-Proxy ist der einzige öffentliche Pfad zu Keycloak. Aufmachen
  (z.B. `0.0.0.0:8081`) nur, wenn das Setup das wirklich braucht.
