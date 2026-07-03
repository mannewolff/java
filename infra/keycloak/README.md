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

- Drei Realm-Rollen: `PENDING` (Default für Selbstregistrierungen), `USER` (Composite mit `offline_access`, #210), `ADMIN`.
- Vier Clients: `toolbox-web` (public, PKCE), `toolbox-ios` (public, PKCE), `toolbox-cli` (public, Device-Flow, siehe [CLI-Login](#cli-login-toolbox-cli)) und `toolbox-api` (bearer-only).
- Self-Registration ist aktiviert, neue User landen in der Rolle `PENDING`.
  Ein Admin schaltet sie zu `USER` frei — siehe [Admin-Approval-Workflow](#admin-approval-workflow).
- Passwort-Policy: mindestens 12 Zeichen, mindestens ein Sonderzeichen, kein Username im Passwort.
- TOTP ist optional pro User (nicht erzwungen).
- Bruteforce-Schutz ist aktiviert.

> **Wichtig — Client-Default-Scopes (#287):** Die tokenausstellenden Clients
> `toolbox-web` und `toolbox-ios` müssen `basic` in ihren `defaultClientScopes`
> führen (vollständig: `web-origins, acr, profile, roles, basic, email`). Der
> `basic`-Scope trägt den Standard-`sub`-Mapper. Fehlt er, enthält das
> Access-Token **kein `sub`** und das Backend bricht jeden authentifizierten
> Request mit `NullPointerException: userSub must not be null` ab (betraf jeden
> neu angelegten User). Beim Editieren der Realm-Exports diese Liste **nicht**
> auf `profile, email` verkürzen.

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

## CLI-Login (toolbox-cli)

Der Client `toolbox-cli` (public, kein Secret) ist für die Kommandozeilen-Anbindung
(`tbx`, Board-Adapter fürs claude-workflow-kit) per **OAuth2 Device Authorization
Grant** (RFC 8628) konfiguriert — analog zu `gh auth login`, ohne Redirect-URI.

**Ablauf:**

1. CLI fordert einen Device-Code an:
   ```bash
   curl -d "client_id=toolbox-cli" -d "scope=openid offline_access" \
     http://localhost:8081/realms/toolbox-dev/protocol/openid-connect/auth/device
   ```
   Antwort enthält `device_code`, `user_code` und `verification_uri`.
2. User öffnet `verification_uri` im Browser, gibt `user_code` ein, loggt sich ein
   und bestätigt.
3. CLI pollt den Token-Endpoint mit dem `device_code`, bis der User bestätigt hat:
   ```bash
   curl -d "grant_type=urn:ietf:params:oauth:grant-type:device_code" \
     -d "client_id=toolbox-cli" -d "device_code=<device_code>" \
     http://localhost:8081/realms/toolbox-dev/protocol/openid-connect/token
   ```
   Bei Scope `offline_access` liefert die Antwort ein **Offline-Refresh-Token**.

**Token-Laufzeit:** Der Realm setzt `offlineSessionIdleTimeout` und
`offlineSessionMaxLifespan` auf `2592000` s (30 Tage). Jede Nutzung (Refresh)
setzt den Idle-Timer zurück — ein aktiv genutztes Offline-Token läuft also nicht
automatisch nach 30 Tagen ab, sondern erst nach 30 Tagen **Inaktivität**.

**Voraussetzung:** Die Realm-Rolle `USER` ist als Composite um `offline_access`
erweitert (#210) — ohne diesen Scope in den Composites verweigert Keycloak dem
User ein Offline-Token, selbst wenn der Client ihn anfordert.

> **Re-Import-Risiko (vgl. #79/#267):** Ein Realm-Re-Import
> (`docker compose up -d --force-recreate keycloak`) überschreibt Rollen- und
> Client-Zuweisungen aus diesem JSON. Nach jedem Re-Import in einer Umgebung mit
> bestehenden Usern: Login testen und prüfen, ob zuvor manuell auf `USER`
> promotete Accounts weiterhin die `USER`-Rolle (inkl. `offline_access`-Composite)
> tragen — der Re-Import ersetzt keine Zuweisungen, die nur in der laufenden
> Keycloak-DB existierten und nicht Teil dieses Exports sind.

## Prod-Setup (Hostinger / eigener Server)

### Voraussetzungen (extern)

1. DNS A/AAAA-Record `toolboxauth.mwolff.org` zeigt auf den Server.
2. Reverse-Proxy (Caddy / Nginx / Traefik / Hostinger-Built-in) leitet
   `https://toolboxauth.mwolff.org:443` auf `127.0.0.1:8081` weiter und setzt
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

### Nachtraegliche Realm-Aenderungen auf einem bestehenden Server anwenden

Sobald der Realm einmal importiert wurde, ueberspringt `--import-realm` ihn bei
jedem weiteren Start komplett (siehe Re-Import-Risiko oben) — ein
`docker compose down -v`, um Aenderungen aus den Realm-JSONs nachzuziehen, ist
auf einem Server mit echten Nutzerdaten **keine Option** (loescht die komplette
MariaDB, nicht nur Keycloak).

Fuer genau diesen Fall gibt es `scripts/keycloak-apply-cli-changes.sh`: wendet
die drei Aenderungen aus #284/#287/#210 (Client `toolbox-cli`, `basic`-Scope
fuer `toolbox-web`/`toolbox-ios`, `USER`-Rolle als Composite mit
`offline_access`) direkt ueber die Admin-REST-API auf einen laufenden Keycloak
an — idempotent, ohne Datenverlust, ohne Neustart.

```bash
KEYCLOAK_URL=https://toolboxauth.mwolff.org \
KEYCLOAK_ADMIN=admin \
KEYCLOAK_ADMIN_PASSWORD=*** \
REALM=toolbox \
./scripts/keycloak-apply-cli-changes.sh --dry-run   # zeigt nur, was fehlt

# nach Pruefung:
./scripts/keycloak-apply-cli-changes.sh --apply
```

Getestet gegen einen Wegwerf-Realm (Anlage-Pfad) und gegen `toolbox-dev`
(Idempotenz-Pfad, alles bereits vorhanden) — siehe Commit-Message für Details.

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

## Custom-Theme `toolbox` (Issue #66)

Login-, Register-, Forgot-Password- und OAuth-Consent-Seiten laufen mit einem
eigenen Toolbox-Theme. Aktivierung erfolgt per Realm-Setting `"loginTheme":
"toolbox"` in beiden Realm-JSONs.

### Layout

```
infra/keycloak/themes/toolbox/login/
├── theme.properties              # parent=base, locales=de,en
├── template.ftl                  # Master-Layout mit Logo + Card + Branding-Block
├── messages/
│   └── messages_de.properties    # Deutsche Labels (Login, Register, etc.)
└── resources/
    ├── css/login.css             # Brand-Petrol #3d8a98, Gradient-Hintergrund
    └── img/logo.png              # Kopie aus frontend/public/logo.png
```

### Mount

`docker-compose.yml` mountet das Verzeichnis als read-only Volume:

```yaml
keycloak:
  volumes:
    - ./infra/keycloak/themes:/opt/keycloak/themes:ro
```

### Deploy auf bestehender Installation

Bei Änderungen am Theme reicht ein Recreate des Keycloak-Containers — kein
Schema-Migration, kein Realm-Re-Import nötig:

```bash
cd ~/opt/java
git pull
docker compose up -d --force-recreate keycloak
```

`--force-recreate` ist Pflicht, weil Compose Volume-Mount-Änderungen erst beim
Recreate übernimmt (Memory: `feedback-docker-compose-up-recreate`).

### Anpassungen

- Brand-Farbe sitzt in `resources/css/login.css` als CSS-Hex `#3d8a98`/`#256270`.
  Wer das ändert, sollte auch `frontend/src/theme.ts` mitziehen.
- Der Branding-Text mit Blog-Link steht in `template.ftl` (Block
  `.toolbox-branding`). Übersetzungen liefern wir hier nicht — der Text ist
  bewusst hardcoded auf Deutsch.
- Logo-Quelle ist `frontend/public/logo.png`; bei Logo-Wechsel beide Stellen
  aktualisieren (oder den Mount auf ein gemeinsames Asset-Verzeichnis
  umstellen).

### Was greift, was nicht

- Login, Register, Forgot-Password, Verify-Email, OAuth-Consent → Toolbox-Theme.
- Account-Self-Service-Page (`/realms/<realm>/account/`) und E-Mail-Templates
  → weiterhin Keycloak-Default. Eigenes `account`- bzw. `email`-Theme ist
  möglich, aktuell nicht im Scope. Falls gewünscht, gleiche Struktur, anderes
  Subverzeichnis.
