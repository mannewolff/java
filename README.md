# api

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
Dockerfile, docker-compose.yml     Lokales Deployment (API + MariaDB + Frontend)
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

Schemamigrationen liegen unter `src/main/resources/db/migration` und werden beim Start von Flyway angewendet (`ddl-auto: validate`).

## Frontend / Dev-Workflow

Drei Startwege je nach Iterationsgeschwindigkeit:

### 1. Dev-Modus (zwei Prozesse, schneller HMR)

```bash
# Terminal 1 – Backend
mvn spring-boot:run -P skip-frontend

# Terminal 2 – Frontend mit Hot Reload
cd frontend
npm install   # nur beim ersten Mal
npm run dev
```

Frontend läuft auf `http://localhost:5173` und leitet `/api/*` per Proxy an Spring auf `:8080` weiter.

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

Der Frontend-Build läuft in einem separaten `node:20`-Stage des Dockerfile und wird in den fat-jar gepackt. API + MariaDB werden zusammen gestartet, `http://localhost:8080` liefert alles.
