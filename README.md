# java goes KI
Dieses Projekt ist vollständig mit KI entwickelt worden. Kein Editor, kein VI, kein nichts, kein gar nichts. Nur ein Prompt und, in diesem Fall, mehrere KI-Modelle. Ziel ist es, eine Toolbox zu bauen, wo ich Dinge, die ich immer und immer wieder brauche, mit abarbeiten kann. Und sind es nur so triviale Dinge wie von einem Icon den Hintergrund transparent zu machen. Es läuft lokal bei mir und – inzwischen auch für mich persönlich – live unter toolbox.mwolff.org, ist aber keine produktive Anwendung für Dritte. Es ist ein Projekt, wo ich zeigen kann, dass auch etwas komplexere Dinge mit KI entwickelt werden können oder eben nicht. 

## API

Spring Boot REST-API mit MariaDB **und React-Frontend (Vite + TS + MUI)** im Stil eines Dashboards (links Menü, rechts Inhalt). Authentifizierung über Keycloak (OIDC/JWT). TDD-Scaffold inkl. Unit-, Slice- und Testcontainers-Integrationstests.

## Stack

- Java 21, Spring Boot 3.5, Maven
- MariaDB 11 (via Docker / Testcontainers)
- Flyway für Schemamigrationen
- Keycloak 26 (eigener Container, Realm-Config als Code in `infra/keycloak/`)
- JUnit 5 · Mockito · AssertJ · Testcontainers · ArchUnit · PIT
- React 18 · TypeScript · Vite · MUI 6 · React Router 6 · `react-grid-layout`
- FastAPI (Python 3.12, uv-managed) für die Image-Pipeline (Pillow, rembg, cairosvg, colorthief)

## Projektstruktur

```
src/main/java/org/mwolff/api/      Application + Domain (auth, dashboard, tools, common)
src/main/resources/                application.yml, Flyway-Migrationen
src/test/java/org/mwolff/api/      Tests (*Test = schnell, *IT = Testcontainers)
frontend/                          React-App (Vite + TS + MUI)
python-tools/                      FastAPI-Microservice (uv + ruff + mypy)
infra/keycloak/                    Realm-Exports (toolbox-dev, toolbox) + Admin-README
infra/mariadb/init/                Init-Scripts (Keycloak-Schema + DB-User)
Dockerfile, docker-compose.yml     Lokales Deployment (api + mariadb + keycloak + python-tools)
```

## Schnellstart

### Tests ausführen

```bash
# Schnelle Unit- und Slice-Tests (keine DB nötig)
mvn test

# Volle Suite inkl. Integrationstests (benötigt Docker-Daemon, startet Testcontainers-MariaDB)
mvn verify
```

### Lokal mit Docker starten

#### Voraussetzungen

- Docker Engine 25+ / Docker Desktop
- Freie Ports: `8080` (API + Frontend), `8081` (Keycloak)
- Bash für die Setup-Scripts unter `scripts/`

#### Schritt 1: Setup

```bash
./scripts/local-dev-setup.sh
```

Das Script erledigt zwei Sachen:

1. Kopiert `.env.example` nach `.env`, falls noch nicht da.
2. Schreibt `frontend/.env.production.local` mit den **lokalen** Keycloak-Werten (`http://localhost:8081`, Realm `toolbox-dev`).

> **Warum dieser Override?** Vite-Frontend wird im Docker-Multi-Stage-Build via `npm run build` gebaut → das ist `mode=production` → Vite lädt `frontend/.env.production` (Production-Keycloak unter `toolboxauth.mwolff.org`). Lokal funktioniert das nicht, weil der Production-Realm `localhost:8080` nicht als Redirect-URI kennt. `frontend/.env.production.local` hat in Vites Auflösungs-Reihenfolge **höhere** Priorität als `.env.production` und überschreibt die Keys nur lokal — die Datei steht in `.gitignore` und kommt nie ins Repo.

> **Wichtig:** Das Script ist idempotent — mehrfaches Ausführen ist safe. Wenn du auf einem Rechner arbeitest, auf dem du zuvor schon mal `local-dev-setup.sh` ausgeführt hast, läuft es einfach erneut durch und überschreibt `frontend/.env.production.local` neu (kein Problem). `frontend/.env.production.local` ist **gitignored** und muss daher nach jedem frischen `git clone` einmalig neu erzeugt werden.

Anschließend **Passwörter in `.env` anpassen** (mindestens `DB_PASSWORD`, `DB_ROOT_PASSWORD`, `KEYCLOAK_ADMIN_PASSWORD`, `KEYCLOAK_DB_PASSWORD`).

#### Schritt 2: Stack starten

```bash
docker compose up --build -d
docker compose ps    # warten bis alle vier Services "healthy" sind
```

Der erste Start dauert ~2 Minuten — Keycloak importiert beim ersten Hochfahren den Realm. Bei laufendem `mariadb-data`-Volume aus früheren Sessions startet alles in ~20 s.

| Service | URL |
|---|---|
| API + Frontend (Spring Boot, ausgeliefertes React-Bundle) | http://localhost:8080 |
| Keycloak (Identity, Realm `toolbox-dev`) | http://localhost:8081 |
| MariaDB | nur intern, `mariadb:3306` |
| python-tools (FastAPI) | nur intern, `python-tools:8000` |

#### Schritt 3: Erst-Login

1. Im Browser `http://localhost:8080` aufrufen → Redirect zu `http://localhost:8081/realms/toolbox-dev/...`.
2. **"Register"** → neuen Account anlegen. Der landet in der Rolle `PENDING` und hat noch keinen Zugriff auf die Toolbox.
3. Als Keycloak-Admin den User promoten:
   - `http://localhost:8081/admin/` → Login mit `KEYCLOAK_ADMIN` / `KEYCLOAK_ADMIN_PASSWORD` aus der `.env`.
   - Realm-Selector oben links auf **`toolbox-dev`** umschalten.
   - `Users` → den neuen User wählen → `Role mapping` → `Assign role` → `USER` zuweisen, `PENDING` entfernen.
4. Im Toolbox-Tab einmal ausloggen + neu einloggen — `http://localhost:8080` zeigt jetzt das Dashboard.

#### Stack stoppen

```bash
docker compose down        # Container weg, DB-Volume bleibt
docker compose down -v     # auch DB-Volume löschen → Erst-Login wieder nötig
```

#### Auf Production-Build umschalten (selten gebraucht)

Falls du lokal den Production-Build testen willst (Frontend zeigt dann auf `toolboxauth.mwolff.org`, was lokal kein gültiger Redirect ist und mit "Invalid parameter" abbricht):

```bash
./scripts/local-dev-teardown.sh    # entfernt frontend/.env.production.local
docker buildx prune -af
docker compose build --no-cache --pull api
docker compose stop api && docker compose rm -f api
docker compose up -d --no-deps api
./scripts/increment-version.sh    # #225: Minor-Version hochzählen (gegen localhost:8080)
```

`./scripts/local-dev-setup.sh` erneut ausführen, um auf den lokalen Auth-Pfad zurückzukommen.

> **Versions-Increment (#225):** Der letzte Deploy-Schritt `./scripts/increment-version.sh` erhöht die Minor-Version. In Produktion ist `api` **nicht** auf einen Host-Port gemappt (nur im Compose-Netz, nginx spricht ihn intern an) — das Skript ruft `POST /api/app/version/increment-minor` daher per Wegwerf-curl-Container **im Netzwerk-Namespace des api-Containers** auf (`docker run --network container:<api>`). Kein Host-Port, kein Reverse-Proxy, keine Firewall/Geo-Filter (die den früheren GitHub-Actions-Job ausgesperrt hatten). Für dev mit Host-Port-Mapping oder Jar ohne Docker: `APP_BASE_URL=http://localhost:8080 ./scripts/increment-version.sh`. Das Repository-Secret `APP_BASE_URL` ist **obsolet** und kann gelöscht werden.
>
> **Auth (#229):** Die increment-Endpunkte verlangen einen Shared-Secret-Header `X-Version-Token`. Setze `APP_VERSION_INCREMENT_SECRET` in der `.env` (gleicher Wert für `api`-Container und Skript) — das Skript liest ihn aus Env oder `.env` und sendet ihn mit. Ohne/falsches Secret antwortet der Endpoint mit 401. `GET /api/app/version` ist seit #229 nur noch für eingeloggte USER; der Health-Poll des Skripts nutzt daher `/actuator/health`.

#### Troubleshooting

**`localhost:8080` zeigt "Invalid parameter: redirect_uri" und URL zeigt auf `toolboxauth.mwolff.org`**

`frontend/.env.production.local` wurde vom Vite-Build nicht eingebrannt. Reproducibility-Check:

```bash
docker build --no-cache --target frontend-build -t test-fb .
docker run --rm test-fb sh -c \
  'grep -oE "toolboxauth\.[a-z]+\.org|localhost:8081" dist/assets/index-*.js | sort -u'
```

- Output `localhost:8081` → Bundle ist OK, der laufende Container hat aber das alte Image. Mit `docker compose stop api && docker compose rm -f api && docker compose up -d --no-deps api` rekreierten.
- Output `toolboxauth.mwolff.org` → Vite hat die `.env.production.local` nicht gelesen. Datei wirklich im richtigen Pfad? `ls -la frontend/.env.production.local`, dann `./scripts/local-dev-setup.sh` neu starten und BuildKit-Cache leeren: `docker buildx prune -af`.

**Spring-Boot crasht mit "Connection refused localhost:3306"**

Container versucht DB unter `localhost` statt unter Service-Name `mariadb` zu erreichen → `DB_HOST` aus `.env` ist nicht im Container angekommen. `docker inspect "$(docker compose ps -q api)" --format '{{range .Config.Env}}{{println .}}{{end}}' | grep DB_` — wenn `DB_HOST=mariadb` fehlt, ist die `.env` nicht im selben Verzeichnis wie `docker-compose.yml`.

**Keycloak hängt mit "Table doesn't exist"**

Volume aus früherem Lauf — Init-Scripts in `infra/mariadb/init/` laufen nur beim allerersten Start. Entweder Keycloak-Schema von Hand anlegen (siehe `infra/keycloak/README.md`) oder `docker compose down -v && docker compose up --build` (DB-Inhalt geht weg).

**Port 8080 / 8081 belegt**

`lsof -iTCP -sTCP:LISTEN -P -n | grep -E ':(8080|8081)'` zeigt den Bewohner. Alternative Ports brauchen Anpassungen in `docker-compose.override.yml`, `frontend/.env.production.local` (Keycloak-URL!) und `SecurityConfig.java` (CORS-Whitelist).

Konkrete Tool-Endpunkte und Beispielaufrufe finden sich im Abschnitt [Tools](#tools).

## REST-API

Die fachlichen Endpunkte liegen unter `/api/tools/...` und werden im Abschnitt [Tools](#tools) beschrieben.

Validation per `@Valid` an Controller-DTOs, Fehler werden vom `GlobalExceptionHandler` als strukturiertes JSON mit `fieldErrors` zurückgegeben.

## TDD-Pyramide

| Typ | Beispiel | Ausführung |
|---|---|---|
| Unit (Mockito + AssertJ) | `*UseCasesTest`, `*Test` in `domain/` | `mvn test` |
| Slice (`@WebMvcTest`) | `*ControllerTest` | `mvn test` |
| Architektur (ArchUnit) | `*LayerArchitectureTest` | `mvn test` |
| Adapter (`@DataJpaTest` + Testcontainers) | `JpaDashboardAdapterIT` | `mvn verify` |
| Smoketest (`@SpringBootTest`) | `ApiApplicationIT` | `mvn verify` |
| Mutation Testing (PIT) | gesamtes Domain/Application | `mvn verify` |

Neue ITs erben von `AbstractIntegrationTest`, dort hängt eine wiederverwendete `MariaDBContainer` mit `@ServiceConnection`. Coverage-Ziel für Domain/Application/Web ist 100/100 (JaCoCo). Persistence-Schicht steht in den JaCoCo-Excludes, weil nur via Testcontainers sinnvoll testbar.

## Konfiguration

Alle DB- und Auth-Werte sind über Umgebungsvariablen mit Defaults parametrisiert:

| Variable | Default (lokal) | Default (Profile `docker`) |
|---|---|---|
| `DB_HOST` | `localhost` | `mariadb` |
| `DB_PORT` | `3306` | `3306` |
| `DB_NAME` | `api` | `api` |
| `DB_USER` | `api` | `api` |
| `DB_PASSWORD` | `api` | `api` |
| `SERVER_PORT` | `8080` | `8080` |
| `PYTHON_TOOLS_URL` | `http://localhost:8000` | `http://python-tools:8000` |
| `KEYCLOAK_ISSUER_URI` | `http://localhost:8081/realms/toolbox-dev` | `http://keycloak:8080/realms/toolbox-dev` |

Schemamigrationen liegen unter `src/main/resources/db/migration` und werden beim Start von Flyway angewendet (`ddl-auto: validate`). Realm-Exports liegen unter `infra/keycloak/` und werden beim ersten Keycloak-Start automatisch importiert.

## Frontend / Dev-Workflow

Drei Startwege je nach Iterationsgeschwindigkeit:

### 1. Dev-Modus (vier Prozesse, schneller HMR)

Keycloak und MariaDB laufen permanent als Container, der Rest läuft nativ für schnellen Reload:

```bash
# Terminal 1 – Infra (MariaDB + Keycloak)
docker compose up mariadb keycloak

# Terminal 2 – Backend
mvn spring-boot:run -P skip-frontend

# Terminal 3 – Frontend mit Hot Reload
cd frontend
npm install   # nur beim ersten Mal
npm run dev

# Terminal 4 – Python-Tools (Image-Pipeline)
cd python-tools
uv sync --frozen   # uv einmalig via `brew install uv` o.ä. installieren
uv run uvicorn main:app --reload
```

Frontend läuft auf `http://localhost:5173` und leitet `/api/*` per Proxy an Spring auf `:8080` weiter. Spring spricht python-tools unter `http://localhost:8000` an (siehe `PYTHON_TOOLS_URL`). Login geht über Keycloak auf `:8081`, Realm `toolbox-dev`.

> **Kein `.env.production.local`-Problem im Dev-Modus.** `npm run dev` nutzt automatisch `frontend/.env.development` (Vite `mode=development`) — dort steht bereits `localhost:8081`. Der Override für den Docker-Build (`frontend/.env.production.local`) ist für diesen Workflow **nicht nötig**.

### 2. Voller Build (ein jar)

```bash
mvn package                                       # baut Frontend + Backend
java -jar target/api-0.0.1-SNAPSHOT.jar
```

Anschließend liefert Spring unter `http://localhost:8080/` sowohl die React-App als auch die JSON-API aus. Direktaufrufe wie `http://localhost:8080/tools/resize` funktionieren dank SPA-Fallback.

Für reine Backend-Iteration (kein npm/Node):

```bash
mvn -P skip-frontend package
```

### 3. Docker

```bash
docker compose up --build
```

`docker compose` startet vier Services: `mariadb`, `keycloak`, `python-tools` (FastAPI mit vorgeladenem rembg-Modell) und `api` (Spring Boot mit eingebettetem React-Bundle). `http://localhost:8080` liefert die React-App, JSON-API und Tool-Endpoints; `http://localhost:8081` Keycloak.

## Features

Alle UI-Routen sind hinter Keycloak-Login und erfordern Rolle `USER`. Backend-Endpunkte sind Bearer-only JWT-protected.

### Dashboards (Hauptfunktion)

| Route | Endpoint | Zweck |
|---|---|---|
| `/dashboards` | `GET/POST /api/dashboards` | Liste, Anlegen, Default markieren, Löschen |
| `/dashboards/:id` | `GET /api/dashboards/{id}`, `PUT /api/dashboards/{id}` | Detail mit Widgets, Layout speichern |
| `/dashboards/:id` (inline rename) | `PUT /api/dashboards/{id}/name` | Inline-Rename |

Widget-Typen: `TEXTBOX` (Markdown + Live-Preview), `KPI` (Number + Trend), `KANBAN_LIST` (Mini-Board-Vorschau). Grid auf `react-grid-layout`, Read/Edit-Modus-Trennung mit Draft-State.

### Kanban-Board

| Route | Endpoint | Zweck |
|---|---|---|
| `/kanban` | `GET/POST/PATCH/DELETE /api/kanban/items` | Vier-Spalten-Board (Backlog → In Progress → In Review → Done) |

Drag & Drop zwischen Spalten und innerhalb einer Spalte (dnd-kit). Items können archiviert (Soft-Delete), wiederhergestellt oder endgültig gelöscht werden. DONE-Items werden nach konfigurierbaren Tagen automatisch bereinigt (`DoneItemCleanupJob`). Kommentarfunktion pro Item. Einstellungen (Retention-Tage, Archiv-Anzeige) über den Settings-Drawer.

### Zeitreihen

| Route | Endpoint | Zweck |
|---|---|---|
| `/timeseries` | `GET/POST /api/timeseries` | Zeitreihen anlegen, Datenpunkte ingesten und visualisieren |

Daten können per Ingest-Token via API ohne Login eingespeist werden (`POST /api/ingest/{token}`).

### Image-Tools

Die Bildtools sind aus dem Hauptmenü in die **Einstellungen** umgezogen (Ausnahme: SVG → PNG und Farbpipette sind weiterhin im Menü). Die Routen bleiben in allen Fällen aktiv.

| Tool | UI-Route | Backend-Endpoint | Implementierung |
|---|---|---|---|
| Hintergrund entfernen | `/tools/remove-background` | `POST /api/tools/remove-background` | Spring proxy → python-tools (rembg / U2Net) |
| Beitragsbild (1200×630) | `/tools/og-image` | `POST /api/tools/crop-og`, `POST /api/tools/palette` | Spring proxy → python-tools (Pillow + colorthief) |
| Bild verkleinern | `/tools/resize` | `POST /api/tools/resize` | Spring proxy → python-tools (Pillow LANCZOS) |
| SVG → PNG | `/tools/svg-to-png` | `POST /api/tools/svg-to-png` | Spring proxy → python-tools (cairosvg) |
| Farbpipette | `/tools/color-picker` | `POST /api/tools/palette` | Spring proxy → python-tools (colorthief) |

### Sonstiges

| Tool | UI-Route | Implementierung |
|---|---|---|
| Passwort-Generator + bcrypt-Hash | `/tools/password` | Rein clientseitig (`bcryptjs`) |

### Smoke-Tests

Bevorzugter Pfad ist der Browser: einloggen über Keycloak, jeweilige `/tools/...`-Route ansteuern, Bild hochladen. Für API-Tests per `curl` braucht es einen Bearer-Token. Da im Realm nur PKCE (`toolbox-web`) und bearer-only (`toolbox-api`) konfiguriert sind, ist der pragmatische Weg: im laufenden Browser die Devtools öffnen, in einem `fetch`-Request den `Authorization`-Header inspizieren und den Token kopieren:

```bash
TOKEN=<aus Browser-Devtools kopiert>

# Hintergrund entfernen
curl -fS -H "Authorization: Bearer $TOKEN" -F file=@icon.png \
     http://localhost:8080/api/tools/remove-background -o icon-transparent.png

# SVG → PNG
curl -fS -H "Authorization: Bearer $TOKEN" -F file=@logo.svg -F width=512 \
     http://localhost:8080/api/tools/svg-to-png -o logo.png
```

Health-Endpoint ist offen (keine Auth):

```bash
curl -s http://localhost:8080/actuator/health
```
