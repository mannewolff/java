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

```bash
cp .env.example .env       # ggf. Passwörter anpassen
docker compose up --build
```

Anschließend laufen vier Services:

| Service | URL |
|---|---|
| API + Frontend (Spring Boot, ausgeliefertes React-Bundle) | http://localhost:8080 |
| Keycloak (Identity, Realm `toolbox-dev`) | http://localhost:8081 |
| MariaDB | localhost:3306 (innerhalb des Netzwerks `mariadb:3306`) |
| python-tools (FastAPI) | innerhalb des Netzwerks `python-tools:8000` |

Health-Check und Login:

```bash
curl -s http://localhost:8080/actuator/health
# Browser: http://localhost:8080 → Login-Redirect zu Keycloak → Self-Register
# Anschließend muss der Admin im Realm den User von PENDING auf USER promoten.
# Details: infra/keycloak/README.md
```

Konkrete Tool-Endpunkte und Beispielaufrufe finden sich im Abschnitt [Tools](#tools).

Stack stoppen:

```bash
docker compose down        # Container weg, Volume bleibt
docker compose down -v     # auch DB-Volume löschen
```

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

Widget-Typen: `TEXTBOX` (Markdown + Live-Preview), `KPI` (Number + Trend). Grid auf `react-grid-layout`, Read/Edit-Modus-Trennung mit Draft-State.

### Image-Tools

| Tool | UI-Route | Backend-Endpoint | Implementierung |
|---|---|---|---|
| Hintergrund entfernen | `/tools/remove-background` | `POST /api/tools/remove-background` | Spring proxy → python-tools (rembg / U2Net) |
| Beitragsbild (1200×630) | `/tools/og-image` | `POST /api/tools/crop-og`, `POST /api/tools/palette` | Spring proxy → python-tools (Pillow + colorthief) |
| Bild verkleinern | `/tools/resize` | `POST /api/tools/resize` | Spring proxy → python-tools (Pillow LANCZOS) |
| SVG → PNG | `/tools/svg-to-png` | `POST /api/tools/svg-to-png` | Spring proxy → python-tools (cairosvg) |

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
