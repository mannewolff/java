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

Neue Issues werden über die Templates in [`.github/ISSUE_TEMPLATE/`](.github/ISSUE_TEMPLATE/) angelegt — `implementation.md` (Backend/Java), `frontend-implementation.md` (React) und `documentation.md` (reine Doku). Die Implementierungs-Templates enthalten die verbindliche Definition-of-Done-Checkliste, damit Architektur-, Test- und Security-Pflichten schon beim Schneiden erzwungen werden.

Issue-Format → siehe [unten](#-issue-dokumentation-format).

### 4. „GO" (User)

**Zentraler Kontrollpunkt.** Erst nach explizitem „GO" / „los" / „mach das" beginnt die Implementierung. ExitPlanMode-Approval allein reicht **nicht**.

Wenn das Projekt ein Kanban-Board nutzt, dient die **Ready**-Spalte als generelles GO — Details siehe [Kanban-Board](#-kanban-board-optionale-steuerung).

### 5. Implementierung gegen das Issue (Claude)

- Das **Issue** ist die Quelle der Wahrheit, nicht der Chat.
- Bei Java: striktes TDD pro [CLAUDE-java.md](CLAUDE-java.md) §8.
- Nach jedem fertigen Issue: lokaler Commit, der das Issue mit **`Refs #N`** referenziert — **niemals** `Closes/Fixes/Resolves #N`. Begründung siehe [Issue-Schließ-Konvention](#-issue-schließ-konvention).
- **Niemals automatisch pushen.**

### 6. Lokale Prüfung (Claude + User)

- `mvn verify` grün (volle Suite inkl. Integration-Tests, Coverage- und Mutationsschwellen erreicht).
- `cd frontend && npm run build` grün.
- Wenn UI: Dev-Server starten, Feature im Browser durchklicken (golden path + Edge-Cases).
- Wenn DB-Schema: Flyway-Migration sauber idempotent? Rollback bei Fehlern?

### 7. Self-Review via `/code-review` (verpflichtend vor jedem Push)

Vor jedem `push main`-Trigger führt Claude den `/code-review`-Skill auf der **gesamten ungepushten Commit-Reihe** (`origin/main..HEAD`) aus — nicht pro Issue, sondern pro Welle. Das verhindert Review-Müdigkeit bei sehr kleinen Issues und fängt die unscheinbaren Verstöße ab, die `mvn verify` nicht abdeckt.

**Trigger-Punkt:** Sagt der User `push main`, antwortet Claude **nicht** direkt mit `git push`, sondern startet zuerst `/code-review`. Erst nach dem Review-Bericht und dessen Bewertung durch den User wird gepusht.

**Findings werden klassifiziert** (P1 / P2 / P3) und im folgenden Format gemeldet:

```
## Review-Bericht (Welle #X)
- P1: <Liste, mit Datei:Zeile + warum kritisch>
- P2: <Liste>
- P3: <Liste>
```

- **P1 blockt den Push.** Claude legt entweder einen Fixup im aktuellen Commit an (wenn HEAD-fix) oder ein neues Issue, das vor dem nächsten Push abgeschlossen sein muss.
- **P2 / P3 blocken nicht.** Claude listet im finalen Push-Abschnitt die Folge-Issue-Nummern auf, die im Backlog landen.

**Externes Zweit-Modell-Review (z. B. OpenAI Codex) bleibt optional** — eine zusätzliche Schicht, wenn der User es manuell drüber laufen lässt, aber nicht der Standard-Pfad. Der Self-Review ersetzt die menschliche Bewertung nicht; er ergänzt sie.

### 8. „Push-Main" (User)

Knappe Anweisung. Erst nach explizitem `push main` (oder gleichwertiger Formulierung) wird gepusht. Push-Ziel klar nennen. Auch wenn das Kanban-Board einen Done-Status hat: Done-Bewegung ist kein Push-Trigger — siehe [Kanban-Board](#-kanban-board-optionale-steuerung).

**`origin/main` wird perspektivisch automatisch auf einen Testserver deployed, auf den auch Kunden gucken.** Ein Push ist deshalb KEIN Implementierungs-Detail, sondern ein Release-Schritt. Auch eine vermeintlich „triviale" CSS-Änderung kann auf dem Testserver Schaden anrichten.

**Regel ohne Ausnahme:** Push erfolgt ausschliesslich, wenn der User in der **aktuellen Antwort** `push main` (oder gleichwertig) sagt. Eine frühere Push-Freigabe in derselben Session gilt **nicht** für nachfolgende Commits — jeder Commit-Batch braucht eine eigene Freigabe.

### 9. Deployment + Pull Request

- Automatischer Deploy auf Test-Server nach `main`-Push.
- User prüft auf Test-Server.
- PR `main` → `production` wird vom User erstellt und gemerged.
- Production-Branch hat Branch-Protection — kein Direkt-Push.

#### PR-Zusammenfassung bei User-Merge

Wenn der User den Merge nach `production` **selbst** macht (z. B. über die GitHub-UI), schreibt Claude **vor dem Merge** eine PR-Zusammenfassung in den Chat:

- **Titel** (kurz, < 70 Zeichen)
- **Beschreibung** mit Summary-Bullets, Test plan, Deploy-Hinweisen

Der Block ist so formatiert, dass der User ihn 1:1 in den GitHub-PR-Dialog kopieren kann.

Wenn der User stattdessen `merge production` als Trigger-Phrase sendet, läuft alles wie bisher: Claude erstellt PR + merged automatisch via `gh pr create` / `gh pr merge`, mit derselben Zusammenfassung im PR-Body.

---

## 📊 Kanban-Board (optionale Steuerung)

Wenn das Projekt ein GitHub-Project-Board nutzt, werden einzelne GO-Trigger aus den 9 Schritten durch Spaltenbewegungen formalisiert. Das Board **ersetzt** die 9 Schritte nicht — es bündelt sie.

### Standard-Spaltenmodell

`Backlog → Ready → In progress → In review → Done`

| Spalte | Bedeutung | Wer schiebt |
|---|---|---|
| **Backlog** | Idee oder Issue mit offenen Designfragen / nicht freigegeben | Beide |
| **Ready** | Vollständig groomt, gilt als generelles GO (Schritt 4) | **Nur User** |
| **In progress** | Aktuelle Arbeit | Claude beim Start |
| **In review** | Lokal fertig, Tests grün, **nicht** gepusht | Claude beim Abschluss |
| **Done** | User hat geprüft + Push erfolgt | **Nur User** |

### Ready-Kriterien

Ein Issue darf nur dann nach Ready, wenn:

- Alle Designfragen geklärt
- Akzeptanzkriterien im Issue stehen
- Scope klein/mittel (1 Feature / 1 Tool, keine Mehrtages-Sache)
- Keine offene Architektur-Entscheidung
- Es ohne Rückfrage durchgezogen werden kann

### Pauschal-Trigger

Eine User-Phrase wie „implementiere alles in Ready" ist das **pauschale GO** (Schritt 4) für alle Ready-Issues. Wochentag egal.

Ablauf pro Issue:

1. Issue von Ready → In progress ziehen
2. Implementieren gem. Schritt 5
3. Lokaler Commit, **nicht** pushen
4. Issue von In progress → In review ziehen
5. Nächstes Ready-Issue, gleicher Ablauf

Wenn Ready leer ist: Meldung an User („alles aus Ready erledigt, N Issues liegen in In review"). **Nicht** eigenmächtig Backlog-Issues nach vorne ziehen.

### Reihenfolge in Ready

Mehrere Issues in Ready → **aufsteigend nach Issue-Nummer** abarbeiten (spiegelt Erstellungsreihenfolge, macht Abhängigkeiten von älteren zu neueren Issues sauber). Wenn ein Issue von einem höher nummerierten abhängt, muss das im Issue **explizit** dokumentiert sein — sonst gilt die Standard-Reihenfolge.

### Done ≠ Push-Trigger

Done-Bewegung im Board ist ein **UI-Signal**, kein Bash-Trigger. Der Push (Schritt 8) bleibt die explizite User-Phrase `push main` — pro Commit-Batch separat. `merge production` bleibt ein **separater** zweiter Trigger.

Der Sicherheits-Default „nichts ohne Trigger-Phrase" gilt unverändert, auch wenn das Board einen Done-Status hat.

### Parallelität

**Ein Issue zur Zeit** in In progress — nicht mehrere parallel. Sonst wird der Review-Schritt unübersichtlich.

### Projekt-spezifische IDs

Project-Nummer, Status-Field-ID und Option-IDs für GraphQL-Mutations sind **projekt-spezifisch** und gehören in die jeweilige projekt-eigene Doku (z.B. `CLAUDE.md` oder lokale Notiz), **nicht** in diese kopierbare Datei. Beim Übertragen in ein neues Projekt nur das Spaltenmodell und die Regeln übernehmen — IDs neu ermitteln via `gh project field-list` und `gh project item-list`.

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
7. **Eine Push-Freigabe gilt pro Commit-Batch, nicht pro Session.** Folge-Commits (auch winzige) brauchen eine neue Freigabe.
8. **Kein `git commit … && git push` in einer Bash-Zeile**, ausser der User hat den Push gerade in dieser Antwort explizit freigegeben. Default ist: commit, dann im Antwort-Text „Push?" anbieten und warten.

### Branch-Strategie (verbindlich)

- **Lokaler Arbeits-Branch ist immer `main`.** Claude checked `main` aus, arbeitet darauf, committed darauf.
- `git checkout main` ist der Standard zu Beginn jeder Session / nach jedem Merge.
- Vor der ersten Änderung in einer Session: `git pull --ff-only` um `main` mit `origin/main` zu synchronisieren.
- **Workflow-Reihenfolge:**
  1. Lokale Commits auf `main`
  2. `push main` (auf explizite User-Freigabe) → `origin/main`
  3. PR `main → production` (Claude erstellt, User oder Claude merged)
- Es gibt **keinen** lokalen `production`-Branch für Entwicklungsarbeit. Der `production`-Branch existiert nur auf GitHub (Branch-Protection) und wird ausschließlich via PR befüllt.

### 🔖 Issue-Schließ-Konvention (verbindlich)

**`main` ist der Default-Branch.** GitHub schließt ein Issue **automatisch**, sobald ein Commit mit `Closes #N` / `Fixes #N` / `Resolves #N` auf dem Default-Branch landet. Die Projekt-Automatik verschiebt geschlossene Items danach nach **Done** — das umgeht die Regel „**Done bewegt nur der User**".

**Regel:**

- In Commit-Messages **immer `Refs #N`** (oder schlicht `#N`) verwenden, **nie** `Closes/Fixes/Resolves #N`. So bleibt das Issue beim `main`-Push offen und in **In review**.
- Das Issue wird erst geschlossen, wenn es in **production** ist: `Closes #N` gehört frühestens in den **PR-Body von `main → production`** — oder der User schließt manuell.
- Der Übergang nach **Done** bleibt damit ausschließlich beim User (bzw. am production-Merge), nie am `main`-Push.

Vorfall 01.06.2026: #153–#157 wurden durch `Closes #N`-Commits beim `main`-Push verfrüht geschlossen und automatisch nach Done geschoben, obwohl sie nur auf dem Testserver (`main`) lagen. Seitdem gilt obige Konvention.

### Pre-Push-Guard (#218)

Ein versionierter Hook unter [`scripts/githooks/pre-push`](scripts/githooks/pre-push) blockt
`git push`, wenn ein in den zu pushenden Commits referenziertes Issue (`#N`) im Project-Board
auf **Backlog** liegt — die mechanische Absicherung der Regel „nur Ready/GO wird umgesetzt".
Aktiviert via `core.hooksPath=scripts/githooks` (setzt `scripts/local-dev-setup.sh`).

- **Backlog-Issue referenziert** → Push abgebrochen. Lösung: Issue nach **Ready** ziehen (GO),
  **nicht** mit `--no-verify` umgehen.
- **gh fehlt / Board nicht erreichbar / Issue nicht am Board** → Warnung, Push läuft (fail-open,
  damit Netzausfälle die Arbeit nicht blockieren).

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
- „Review-Skip, weil die Welle klein wirkt." (Nein — Reviews fangen genau die unscheinbaren Verstöße ab.)
- „Direkt pushen, weil `mvn verify` grün ist." (Nein — JaCoCo + ArchUnit decken nicht alles ab: Naming, Schicht-Drift in DTOs, fehlende `@Valid`. Erst `/code-review`.)

---

## 🔗 Weiterführende Docs

- [CLAUDE.md](CLAUDE.md) — Projekt-Übersicht
- [CLAUDE-java.md](CLAUDE-java.md) — TDD, Coverage, Architektur
- [CLAUDE-react.md](CLAUDE-react.md) — Frontend-Regeln
- [CLAUDE-security.md](CLAUDE-security.md) — Sicherheit
- Blog-Artikel zum Workflow: https://blog.mwolff.org/wie-ich-mit-ki-arbeite-mein-workflow-vom-gedanken-bis-zur-produktion/
