# CLAUDE-workflow.md — Workflow, Plan-Mode & Git

Verbindlicher Prozess für jede nicht-triviale Aufgabe. Basiert auf dem 9-Schritte-Workflow „Vom Gedanken bis zur Produktion" (siehe https://blog.mwolff.org/wie-ich-mit-ki-arbeite-mein-workflow-vom-gedanken-bis-zur-produktion/).

---

## 🧭 Grundsatz

**Die KI strukturiert, formuliert und implementiert. Der Mensch ordnet ein, gibt frei und trägt Verantwortung.**

Geschwindigkeit ist nicht der primäre Wert — Klarheit und Kontrolle sind es. Jeder Schritt im Workflow dient genau diesem Zweck. Wer Schritte überspringt, untergräbt das Prinzip.

---

## 🔄 Die 9 Schritte

### 1. Anforderungsdefinition (User)

Der User diktiert oder formuliert die Anforderung. Wenn ein Satz durch Spracherkennung abbricht oder Lücken hat: **nachfragen, nicht raten.**

### 2. Plan (Claude)

- Ziel, Nutzerwirkung und betroffene Bereiche verstehen.
- Relevante Projektdateien und bestehende Muster lesen (Explore-Agent, wenn Scope unklar).
- Konkreten Implementierungsplan erstellen: Kontext, Architektur, betroffene Dateien, Verifikation.
- **Nicht implementieren, bevor der Plan freigegeben und in Issue(s) überführt ist.**

Plan-Mode wird über `ExitPlanMode` verlassen — das Plan-Approval ist die Voraussetzung für Schritt 3, **nicht** die Freigabe zur Implementierung.

### 3. Plan → GitHub-Issue(s) (Claude, mit User)

Plan wird in **kleinteilige** GitHub-Issues überführt — eines pro logischer Einheit. Jedes Issue ist selbst-erklärend (Kontext, Aufgabe, Akzeptanzkriterium, Abhängigkeiten) und steht ohne Chat-Verlauf für sich.

Issue-Format → siehe [unten](#-issue-dokumentation-format).

### 4. „GO" (User)

**Zentraler Kontrollpunkt.** Erst nach explizitem „GO" / „los" / „mach das" beginnt die Implementierung. ExitPlanMode-Approval allein reicht **nicht**.

### 5. Implementierung gegen das Issue (Claude)

- Das **Issue** ist die Quelle der Wahrheit, nicht der Chat.
- Bei Java: striktes TDD pro [CLAUDE-java.md](CLAUDE-java.md) §8.
- Nach jedem fertigen Issue: lokaler Commit mit Bezug `Closes #N`.
- **Niemals automatisch pushen.**

### 6. Lokale Prüfung (Claude + User)

- `mvn verify` grün (volle Suite inkl. Integration-Tests, Coverage- und Mutationsschwellen erreicht).
- `cd frontend && npm run build` grün.
- Wenn UI: Dev-Server starten, Feature im Browser durchklicken (golden path + Edge-Cases).
- Wenn DB-Schema: Flyway-Migration sauber idempotent? Rollback bei Fehlern?

### 7. Code-Review durch zweites Modell (User-initiiert)

Vier-Augen-Prinzip mit zwei KI-Modellen. Standard-Setup: Claude implementiert, OpenAI Codex reviewt (oder analoge Tools). Der Review-Pass ergänzt die menschliche Bewertung — er ersetzt sie nicht.

### 8. „Push-Main" (User)

Knappe Anweisung. Erst nach explizitem `push main` (oder gleichwertiger Formulierung) wird gepusht. Push-Ziel klar nennen.

### 9. Deployment + Pull Request

- Automatischer Deploy auf Test-Server nach `main`-Push.
- User prüft auf Test-Server.
- PR `main` → `production` wird vom User erstellt und gemerged.
- Production-Branch hat Branch-Protection — kein Direkt-Push.

---

## 🔒 Git-Workflow (strikt bindend)

### Ziel

Lokale Kontrolle vor jedem Push. Production ausschließlich via Pull Request.

### Regeln

1. **Commits lokal**, niemals automatisch pushen.
2. **`production` wird niemals direkt gepusht.**
3. **Force-Push** auf `main` oder `production` ist nicht erlaubt, außer auf explizite, einzeln formulierte Anweisung.
4. **Hooks** (Pre-Commit/Pre-Push) werden nicht mit `--no-verify` umgangen. Bei Hook-Fehler: Ursache fixen, nicht umgehen.
5. Wenn der User „push" sagt, ohne Ziel zu nennen: **nachfragen**, ob `main` oder `production` gemeint ist (Default: `main` nur).
6. Solo-Dev-Modus: Commits gehen direkt auf `main` (keine Feature-Branches, wenn nicht ausdrücklich gewünscht). Worktree-Branches sind eine Ausnahme — beim Abschluss in `main` fast-forwarden.

### Warnsignale (nicht ignorieren)

- Fehlgeschlagene Branch-Protection-Regeln
- „Bypassed rule violations"-Meldungen aus dem Remote
- Explizite „bitte nicht pushen"-Anweisungen des Users

---

## ✅ Pflichtchecks vor Abschluss

```bash
# Backend
mvn verify                              # Tests, Coverage, Mutation, ArchUnit, Static Analysis

# Frontend
cd frontend && npm run build            # tsc -b && vite build
```

Wenn der Build E2E-Charakter hat (Docker, lokales Backend gegen lokales Frontend): vor dem Push beide Seiten **manuell starten und prüfen**. Wenn ein Check nicht ausführbar ist (kein Docker-Daemon, kein DB-Container), das in der Abschluss-Notiz **explizit** vermerken und nicht als „passt schon" verkaufen.

---

## 📋 Abschlussbericht (Format)

```text
Änderungen
- <Datei 1> — <kurze Wirkung>
- <Datei 2> — <kurze Wirkung>

Tests und Checks
- <Kommando 1> → <Ergebnis>
- <Kommando 2> → <Ergebnis>

Hinweise
- <verbleibende Risiken, offene Punkte, manuelle Folgeschritte>
```

Der Bericht muss konkret sein. Pfade, Kommandos, Entscheidungen und Restrisiken benennen. Keine schwammigen „funktioniert"-Aussagen.

---

## 📄 Issue-Dokumentation (Format)

```markdown
# [Titel — knapp, < 80 Zeichen]

## Kontext

[Warum diese Aufgabe gemacht wird. Vorgeschichte. Was vorher fehlt.]

## Aufgabe

[Was konkret zu tun ist. Dateien, Klassen, Komponenten. Wenn Tests vorher zu schreiben sind — TDD-Liste hier.]

## Akzeptanzkriterium

[Wie man verifiziert, dass die Aufgabe erledigt ist. Konkret, mess- oder ausführbar.]

## Abhängigkeiten

[Andere Issues, die zuerst fertig sein müssen, oder „Keine".]
```

Issues sind **kleinteilig**. Ein Issue = ein logischer Schritt, der eigenständig getestet werden kann.

---

## 📂 Worktree-Strategie

- **Kontinuierliche zusammenhängende Tasks** (Content-Updates, kleine Fixes, Feature-Ergänzungen): gleicher Worktree.
- **Strukturelle Änderungen / neue Major-Features**: neuer Worktree + neue Session.
- Beim Abschluss eines Worktrees: Branch mit `git merge --ff-only` in `main` zusammenführen, dann pushen (auf Anweisung). Worktree danach räumen.

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

Wenn ein Punkt höher gewichtet werden soll: ausdrücklich anfordern und im Issue/Commit begründen.

---

## 🧪 Test- & Quality-Standards

### Backend (Java)

Siehe [CLAUDE-java.md](CLAUDE-java.md) §3–§5. Pflicht-Kennzahlen:

- JaCoCo: 100 % Line + Branch Coverage
- PIT: 100 % Mutation Score + Test Strength
- ArchUnit: grün
- Spotless / Checkstyle / SpotBugs / PMD / Error Prone: keine Findings

### Frontend (React)

- Neue oder geänderte Logik braucht Vitest-Tests.
- Kritische UI-Zustände abdecken: Loading, Error, Empty, Success, Disabled.
- React Testing Library für Verhaltens-Tests; keine Snapshot-Spielereien ohne Mehrwert.

### Wenn KEINE Tests möglich

- Begründung im Abschlussbericht (z. B. „kein Docker-Daemon verfügbar, E2E-Verifikation aufgeschoben").
- Manuelle Prüfanweisung mit konkreten Schritten beilegen.

---

## 🔐 Security-Checks vor Merge

Vollständige Liste siehe [CLAUDE-security.md](CLAUDE-security.md) („Security-Checklist vor Commit"). Kurzfassung:

- Keine Secrets im Diff
- Alle SQL/JPQL-Queries mit Parameter-Bindung
- `@Valid` auf Controller-DTOs
- Keine Debug-Ausgaben (`System.out.println`, `console.log` mit sensiblen Daten)
- Keine zusätzlich exponierten Actuator-Endpunkte ohne Auth

---

## 🚫 Inakzeptable Verhaltensweisen

Wenn du dich beim Arbeiten dabei ertappst, einen dieser Gedanken zu haben — **stoppen**:

- „Plan-Approval reicht, ich fange einfach an." (Nein — Issue + GO sind Pflicht.)
- „Ich pushe schnell, der User merkt's." (Niemals ohne Anweisung pushen.)
- „Die Tests folgen später." (Nein — TDD heißt Test zuerst.)
- „Coverage ist bei 98 %, das reicht." (Reicht nicht — 100 % oder begründeter Ausschluss.)
- „Ich erfinde mal den nächsten Schritt." (Nein — beim Issue bleiben, bei Unklarheit nachfragen.)
- „Der Hook ist nervig, `--no-verify`." (Nein — Hook-Ursache fixen.)

---

## 🔗 Weiterführende Docs

- [CLAUDE.md](CLAUDE.md) — Projekt-Übersicht
- [CLAUDE-java.md](CLAUDE-java.md) — TDD, Coverage, Architektur
- [CLAUDE-react.md](CLAUDE-react.md) — Frontend-Regeln
- [CLAUDE-security.md](CLAUDE-security.md) — Sicherheit
- Blog-Artikel zum Workflow: https://blog.mwolff.org/wie-ich-mit-ki-arbeite-mein-workflow-vom-gedanken-bis-zur-produktion/
