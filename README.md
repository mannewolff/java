# java goes KI
Dieses Projekt ist vollständig mit KI entwickelt worden. Kein Editor, kein VI, kein nichts, kein gar nichts. Nur ein Prompt und, in diesem Fall, mehrere KI-Modelle. Ziel ist es, eine Toolbox zu bauen, wo ich Dinge, die ich immer und immer wieder brauche, mit abarbeiten kann. Und sind es nur so triviale Dinge wie von einem Icon den Hintergrund transparent zu machen. Dieses Tool ist nicht fürs Deployment gedacht. Es läuft lokal bei mir und hilft mir. Es ist ein Projekt, wo ich zeigen kann, dass auch etwas komplexere Dinge mit KI entwickelt werden können oder eben nicht. 

## API

Spring Boot REST-API mit MariaDB **und React-Frontend (Vite + TS + MUI)** im Stil eines Dashboards (links Menü, rechts Inhalt). TDD-Scaffold inkl. Unit-, Slice- und Testcontainers-Integrationstests.

## Stack

- Java 21, Spring Boot 3.5, Maven
- MariaDB (via Docker / Testcontainers)
- Flyway für Schemamigrationen
- JUnit 5 · Mockito · AssertJ · Testcontainers
- React 18 · TypeScript · Vite · MUI 6 · React Router 6

## Projektstruktur

```
src/main/java/org/mwolff/api/      Application + Domain
src/main/resources/                application.yml, Flyway-Migrationen
src/test/java/org/mwolff/api/      Tests (*Test = schnell, *IT = Testcontainers)
frontend/                          React-App (Vite + TS + MUI)
python-tools/                      FastAPI-Microservice (rembg, Pillow)
Dockerfile, docker-compose.yml     Lokales Deployment (API + MariaDB + python-tools + Frontend)
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

Anschließend ist die API unter `http://localhost:8080` erreichbar:

```bash
curl -s http://localhost:8080/actuator/health
curl -s -X POST http://localhost:8080/api/books \
     -H 'Content-Type: application/json' \
     -d '{"title":"Effective Java","author":"Bloch","isbn":"978-0134685991"}'
curl -s http://localhost:8080/api/books
```

Stack stoppen:

```bash
docker compose down        # Container weg, Volume bleibt
docker compose down -v     # auch DB-Volume löschen
```

## REST-API

| Methode | Pfad              | Beschreibung           |
|---------|-------------------|------------------------|
| GET     | `/api/books`      | Liste aller Bücher     |
| GET     | `/api/books/{id}` | Einzelnes Buch (404 bei Unbekannt) |
| POST    | `/api/books`      | Neues Buch (201 + Location) |
| PUT     | `/api/books/{id}` | Buch aktualisieren     |
| DELETE  | `/api/books/{id}` | Buch löschen (204)     |

Validation per `@Valid` → Fehler werden vom `GlobalExceptionHandler` als strukturiertes JSON mit `fieldErrors` zurückgegeben.

## TDD-Pyramide

| Datei | Typ | Ausführung |
|---|---|---|
| `BookServiceTest` | Unit (Mockito) | `mvn test` |
| `BookControllerTest` | `@WebMvcTest` Slice | `mvn test` |
| `BookRepositoryIT` | `@DataJpaTest` + Testcontainers | `mvn verify` |
| `BookApiIT` | `@SpringBootTest` end-to-end | `mvn verify` |
| `ApiApplicationIT` | Context-Loads-Smoketest | `mvn verify` |

Neue ITs erben von `AbstractIntegrationTest` — dort hängt eine wiederverwendete `MariaDBContainer` mit `@ServiceConnection`.

## Konfiguration

Alle DB-Werte sind über Umgebungsvariablen mit Defaults parametrisiert:

| Variable | Default (lokal) | Default (Profile `docker`) |
|---|---|---|
| `DB_HOST` | `localhost` | `mariadb` |
| `DB_PORT` | `3306` | `3306` |
| `DB_NAME` | `api` | `api` |
| `DB_USER` | `api` | `api` |
| `DB_PASSWORD` | `api` | `api` |
| `SERVER_PORT` | `8080` | `8080` |
| `PYTHON_TOOLS_URL` | `http://localhost:8000` | `http://python-tools:8000` |

Schemamigrationen liegen unter `src/main/resources/db/migration` und werden beim Start von Flyway angewendet (`ddl-auto: validate`).

## Frontend / Dev-Workflow

Drei Startwege je nach Iterationsgeschwindigkeit:

### 1. Dev-Modus (drei Prozesse, schneller HMR)

```bash
# Terminal 1 – Backend
mvn spring-boot:run -P skip-frontend

# Terminal 2 – Frontend mit Hot Reload
cd frontend
npm install   # nur beim ersten Mal
npm run dev

# Terminal 3 – Python-Tools (für Hintergrund-Entfernung)
cd python-tools
python3 -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
uvicorn main:app --reload
```

Frontend läuft auf `http://localhost:5173` und leitet `/api/*` per Proxy an Spring auf `:8080` weiter. Spring spricht python-tools unter `http://localhost:8000` an (siehe `PYTHON_TOOLS_URL`).

### 2. Voller Build (ein jar)

```bash
mvn package                                       # baut Frontend + Backend
java -jar target/api-0.0.1-SNAPSHOT.jar
```

Anschließend liefert Spring unter `http://localhost:8080/` sowohl die React-App als auch die JSON-API aus. Direktaufrufe wie `http://localhost:8080/books` funktionieren dank SPA-Fallback.

Für reine Backend-Iteration (kein npm/Node):

```bash
mvn -P skip-frontend package
```

### 3. Docker

```bash
docker compose up --build
```

`docker compose` startet vier Services: MariaDB, python-tools (FastAPI mit vorgeladenem rembg-Modell), API (Spring Boot mit eingebettetem Frontend) und das Frontend-Build innerhalb des API-Images. `http://localhost:8080` liefert die React-App, JSON-API und Tool-Endpoints.

## Tools

Persönliche Toolbox-Funktionen, jeweils unter `/tools/...` im UI und `/api/tools/...` im Backend erreichbar.

| Tool | UI-Route | Backend-Endpoint | Implementierung |
|---|---|---|---|
| Hintergrund entfernen | `/tools/remove-background` | `POST /api/tools/remove-background` | Spring proxy → python-tools (rembg / U2Net) |

Smoke-Test gegen das Backend (bei laufendem Docker-Stack):

```bash
curl -fS -F file=@icon.png http://localhost:8080/api/tools/remove-background -o icon-transparent.png
```
