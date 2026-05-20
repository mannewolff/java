# CLAUDE.md — Projekt-Standards

Diese Datei ist der Einstiegspunkt für alle Engineering-Regeln in diesem Projekt. Sie definiert den Mindeststandard — Abweichungen sind Fehler und müssen vor dem Abschluss einer Aufgabe korrigiert werden.

---

## 🎯 Schnelleinstieg

- **Neu im Projekt?** Lies diese Datei + [CLAUDE-workflow.md](CLAUDE-workflow.md).
- **Java/Spring-Backend arbeiten?** → [CLAUDE-java.md](CLAUDE-java.md)
- **React-Frontend arbeiten?** → [CLAUDE-react.md](CLAUDE-react.md)
- **Security?** → [CLAUDE-security.md](CLAUDE-security.md)
- **Plan-Mode / Git / Issue-Workflow?** → [CLAUDE-workflow.md](CLAUDE-workflow.md)

---

## 📚 Guide-Familie

| Guide | Fokus | Wiederverwendbar |
|---|---|---|
| **CLAUDE.md** (diese Datei) | Projekt-Übersicht + Pflichtchecks | ❌ Projekt |
| [CLAUDE-java.md](CLAUDE-java.md) | Java 21, Spring Boot 3, TDD, Coverage, Mutationstests | ✅ Allgemein |
| [CLAUDE-react.md](CLAUDE-react.md) | React 18, Vite, TypeScript, MUI | ✅ Allgemein |
| [CLAUDE-security.md](CLAUDE-security.md) | Spring Security, JPA, Frontend-XSS, Secrets | ✅ Allgemein |
| [CLAUDE-workflow.md](CLAUDE-workflow.md) | 9-Schritte-Workflow, Issues, Git, Pflichtchecks | ✅ Allgemein |

---

## 🌐 Projektkontext

**Ziel:** Web-Anwendung mit Java-Backend und React-Frontend im Stil eines Dashboards (linkes Navigationsmenü, rechter Inhaltsbereich, vergleichbar mit Claude Desktop / Hetida).

**Stack:**

| Schicht | Technologie |
|---|---|
| Backend-Sprache | Java 21 (LTS) |
| Backend-Framework | Spring Boot 3.5, Spring Data JPA, Spring Web |
| Build (Backend) | Maven |
| Datenbank | MariaDB 11 (Postgres-kompatible Patterns) |
| Schema-Migrationen | Flyway |
| Test (Backend) | JUnit 5, AssertJ, Mockito, Testcontainers, ArchUnit, PIT |
| Frontend-Sprache | TypeScript (`strict: true`) |
| Frontend-Framework | React 18, React Router 6 |
| Frontend-Build | Vite 5 |
| UI-Library | Material UI 6 (MUI) + Emotion |
| Test (Frontend) | Vitest + React Testing Library |
| Containerisierung | Docker (Multi-Stage: Node + Maven + JRE) |

**Verbindung Frontend↔Backend:** Im Dev leitet der Vite-Dev-Server `/api/*` an Spring Boot auf `:8080` weiter. In Produktion serviert Spring Boot den React-Build aus `classpath:/static/`; eine Domain, kein CORS.

---

## 📂 Projektstruktur

```
/
├── CLAUDE*.md                          # Diese Guide-Familie
├── pom.xml                             # Maven-Konfiguration (inkl. frontend-maven-plugin)
├── Dockerfile, docker-compose.yml      # Multi-Stage-Image + lokale Composition
├── .env.example                        # DB-Credentials-Vorlage
├── src/main/java/org/mwolff/api/       # Application + Domain
│   ├── ApiApplication.java
│   ├── book/                           # Beispiel-Domäne (Books-CRUD)
│   └── common/                         # GlobalExceptionHandler, SpaForwardingController
├── src/main/resources/                 # application.yml + Flyway-Migrationen
│   └── db/migration/                   # V1__…sql, V2__…sql, … (Flyway-Konvention)
├── src/test/java/org/mwolff/api/       # Tests (*Test = Unit/Slice, *IT = Testcontainers-Integration)
└── frontend/                           # React-App
    ├── package.json, vite.config.ts, tsconfig*.json
    ├── index.html
    └── src/
        ├── main.tsx, App.tsx, theme.ts
        ├── layout/                     # AppShell, navItems
        ├── pages/                      # DashboardPage, BooksPage, SettingsPage
        └── api/                        # client.ts (fetch-Wrapper), <domain>.ts
```

---

## ✅ Pflichtchecks vor Abschluss einer Aufgabe

```bash
# Backend
mvn verify                              # Tests + Coverage + Mutation (siehe CLAUDE-java.md §5)

# Frontend
cd frontend && npm run build            # tsc -b && vite build
```

Verfahren, Reporting-Format und detaillierte Schritte → [CLAUDE-workflow.md](CLAUDE-workflow.md).

---

## ⚠️ Prioritäten bei Zielkonflikten

1. **Sicherheit**
2. **Korrektheit**
3. **Datenintegrität**
4. **Accessibility**
5. **Wartbarkeit**
6. **Testbarkeit**
7. **Performance**
8. **Visuelle Präferenz**
9. **Bequemlichkeit der Implementierung**

Keine kurzfristige Bequemlichkeit rechtfertigt unsicheren, untypisierten oder schwer wartbaren Code. Wenn Sicherheit gegen Performance abgewogen wird, gewinnt Sicherheit. Wenn Korrektheit gegen Geschwindigkeit der Lieferung abgewogen wird, gewinnt Korrektheit.

---

## 📐 Verhältnis der Guides untereinander

- **CLAUDE.md** ist die Übersicht. Konflikte zwischen den Sub-Guides werden hier geklärt.
- **CLAUDE-java.md** und **CLAUDE-react.md** beschreiben die schichtspezifischen Engineering-Regeln. Bei Widerspruch zur Sicherheit gewinnt [CLAUDE-security.md](CLAUDE-security.md).
- **CLAUDE-security.md** hat in allen Sicherheitsfragen Vorrang.
- **CLAUDE-workflow.md** beschreibt das Prozess-Drumherum (Plan-Mode, Issues, Commits, GO-Freigabe, Tests). Wer Code schreibt ohne den Workflow zu befolgen, hat die Aufgabe nicht abgeschlossen.

---

## 🗑️ Historische Hinweise

Die früher in diesem Verzeichnis liegende `CLAUDE-content.md` aus dem mwolff.org-Projekt wurde entfernt — das beschriebene Dual-Source-Content-System (JSON-Defaults + DB-Overrides + `EditableText`-Komponente + Inline-Admin-Editor) existiert in diesem Projekt nicht und gehört zu einer fremden Architektur. Falls ein vergleichbares Pattern später benötigt wird, wird ein neuer Guide auf Basis der dann gewählten Java-/React-Implementierung geschrieben.

---

**TL;DR:** Java 21 + Spring Boot 3 (TDD-pflichtig, 100 % Coverage). React 18 + TypeScript strict + MUI. Sicherheit > Korrektheit > Komfort. Vor jedem Push: `mvn verify` und `npm run build` grün. Plan-Mode und GitHub-Issues sind verbindlich (siehe Workflow).
