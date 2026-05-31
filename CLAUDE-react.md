# CLAUDE-react.md — React / Vite / TypeScript / MUI

Verbindliche Regeln für das Frontend (`frontend/src/`). Ergänzend zu [CLAUDE.md](CLAUDE.md) und [CLAUDE-security.md](CLAUDE-security.md). Java-/Backend-Pendant: [CLAUDE-java.md](CLAUDE-java.md).

---

## 🏗️ Frontend-Stack

- **React 18** (funktionale Komponenten, Hooks, keine Class-Components)
- **Vite 5** (kein CRA, keine zusätzliche Webpack-Konfiguration)
- **TypeScript 5+** mit `strict: true`
- **React Router 6** (BrowserRouter, flache Routen)
- **Material UI 6 (MUI)** + Emotion für Styling
- **Vitest + React Testing Library** für Tests
- **ESLint 10** (`eslint.config.js` flat config) mit TypeScript, React, React-Hooks, jsx-a11y

Der Dev-Server (Vite, `:5173`) leitet `/api/*` per Proxy an Spring Boot (`:8080`). In Produktion serviert Spring Boot den Vite-Build aus `classpath:/static/`. Eine Domain, kein CORS.

---

## 📘 TypeScript

- `strict: true`, `noUnusedLocals`, `noUnusedParameters`, `noFallthroughCasesInSwitch` sind aktiviert.
- `any` ist verboten, außer es gibt einen zwingenden Grund. Begründung dann im Code dokumentieren.
- Verwende `unknown` für extern stammende Daten und engere sie über Type-Guards ein.
- Discriminated Unions für fachliche Varianten.
- Keine `as`-Casts zur Umgehung von Modellfehlern. Datenmodell korrigieren statt unterdrücken.
- Keine Non-Null-Assertions (`!`) aus Bequemlichkeit. Ausnahme: `document.getElementById('root')!` in `main.tsx` ist akzeptabel.
- Props explizit typisieren. API-Responses an der Systemgrenze typisieren und auf interne Modelle mappen — die TypeScript-Typen unter `frontend/src/api/` müssen mit den Java-DTOs übereinstimmen.

---

## 🔧 Vite

- Umgebungsvariablen über `import.meta.env`. Nur `VITE_`-Prefix ist im Client sichtbar.
- **Keine** Secrets in `VITE_*` — alles dort ist potenziell öffentlich (siehe [CLAUDE-security.md](CLAUDE-security.md)).
- `vite.config.ts` nur ändern, wenn unvermeidbar. Insbesondere die Proxy-Konfiguration für `/api` bleibt stabil, damit Dev- und Prod-Verhalten symmetrisch sind.
- Neue Vite-Plugins nur mit erkennbarem Nutzen.

### Performance-Budget

Messung 2026-05-31 nach Einführung von Route-Level Lazy Loading:

| Chunk | Größe (minified) | Gzip | Limit | Status |
|---|---|---|---|---|
| `index.js` (Vendor) | ~485 kB | ~152 kB | 600 kB | ✅ Floor: React + MUI + Router + OIDC |
| `DashboardPage.js` | ~528 kB | ~146 kB | 600 kB | ✅ Dokumentierte Ausnahme: Widget-Registry |
| Alle anderen Route-Chunks | < 130 kB | < 40 kB | 600 kB | ✅ |

**Regeln:**
- **Route-Level Lazy Loading ist Pflicht** für alle Top-Level-Routen in `App.tsx` (via `React.lazy` + `Suspense`). Kein direktes Import einer Page-Komponente in `App.tsx` ohne `lazy()`.
- **Neuer Chunk > 600 kB** → Pflicht-Review: lässt sich die Komponente aufteilen? Wenn nein, dokumentierter Ausnahmefall in dieser Tabelle.
- **Vendor-Bundle** (`index.js`) wird durch MUI-Basis bestimmt. Keine zusätzlichen Abhängigkeiten ins Vendor-Bundle einschleppen, ohne Größe zu prüfen.
- `build.chunkSizeWarningLimit: 600` in `vite.config.ts` ist die Grenze für Build-Warnungen — entspricht dem dokumentierten Budget.

---

## ⚛️ React-Komponenten

### Größe & Struktur

- Eine Verantwortung pro Komponente.
- Große JSX-Blöcke und tiefe Bedingungen in benannte Hilfskomponenten extrahieren.
- Geschäftslogik gehört nicht ins JSX, sondern in dedizierte Module/Hooks.

### Props

- Minimal, eindeutig, stabil.
- Keine ungeplante Kombination mehrerer Booleans — stattdessen Union-Type-Varianten.
- `children` nutzen, wenn Komposition natürlicher ist als ein Prop-Sumpf.

### Code-Organisation

Aktuelle Struktur unter `frontend/src/`:

- `layout/` — App-übergreifende Shell (`AppShell`, `navItems`). AppBar oben, permanenter Drawer links, Main-Bereich mit `<Outlet />`.
- `pages/` — Routen-Komponenten (`DashboardPage`, `SettingsPage`, tool-spezifische Seiten unter `pages/tools/`).
- `api/` — Typisierte API-Aufrufe (`client.ts` als fetch-Wrapper, je Domäne eine eigene Datei).
- `theme.ts` — MUI-Theme zentral.
- `main.tsx` — React-Root, `BrowserRouter`, `ThemeProvider`, `CssBaseline`.
- `App.tsx` — `<Routes>` innerhalb `<AppShell>`.

Neue wiederverwendbare UI-Bausteine: eigener Ordner unter `frontend/src/components/` (anlegen, sobald nötig).

---

## 🎨 MUI / Styling

- **MUI ist die primäre UI-Library.** Vor dem Anlegen einer eigenen Komponente prüfen, ob MUI das schon liefert.
- **Theme zentral** in `theme.ts`. Farben, Spacing, Border-Radius über das Theme, nicht hartcodiert. `useTheme()` oder `sx={{ ... }}` mit Theme-Funktionen für Token-Zugriff.
- **`sx`-Prop bevorzugen** für komponentennahe Styles. Inline-Style-Objekte (`style={{ ... }}`) nur für dynamische Einzelwerte ohne Theme-Bezug.
- **Keine zweite Styling-Library** (kein Tailwind, kein styled-components on top, keine eigenen CSS-Module ohne klaren Grund).
- **CSS-Globals** sind tabu. Globaler Reset kommt aus `<CssBaseline />`.
- **Responsiveness** über `theme.breakpoints` (`xs`, `sm`, `md`, …) bzw. `useMediaQuery`. Layouts werden mobile-first geschrieben.

---

## 🪝 Hooks

- Nur auf Top-Level aufrufen, nie in Schleifen oder Bedingungen.
- Eigene Hooks beginnen mit `use…` und haben **eine** klare Verantwortung.
- `useEffect` ist kein Standardwerkzeug für Datenableitung — nur für echte Seiteneffekte (Subscriptions, Timer, Netzwerk, DOM, Synchronisation).
- Dependency-Arrays vollständig und ehrlich. Lint-Regeln dürfen nicht stillschweigend umgangen werden.
- Cleanups (Subscriptions, Timer, Listener) müssen aufgeräumt werden.
- Asynchrone Effekte müssen Race-Conditions verhindern (Cancellation-Flag oder `AbortController`).

---

## 🎛️ State Management

- Lokaler UI-State bleibt lokal.
- `useReducer` ab dem Punkt, an dem mehrere Übergänge zusammengehören.
- Globaler State nur, wenn mehrere entfernte Bereiche dieselbe Source of Truth brauchen. Vor der Einführung eines neuen Contexts oder einer State-Library (Zustand, Redux Toolkit, …): rechtfertigen, warum lokal nicht reicht.
- Server-State ist kein UI-State — keine Caches doppelt halten. Wenn der Server die Wahrheit ist, ist der Server zu fragen.
- URL-State bevorzugen, wenn ein Zustand teilbar oder navigationsrelevant ist (`useParams`, `useSearchParams`).

---

## 🌐 Datenzugriff & APIs

- Externer Input ist unsicher, bis er validiert und gemappt wurde.
- API-Aufrufe gehören in `frontend/src/api/` (aktuell `client.ts` + Domänen-Module wie `resize.ts`, `ogImage.ts`), nicht direkt in Komponenten.
- **Fehlerbehandlung an der Quelle:** Der `client.ts`-Wrapper wirft `ApiError` mit Statuscode + Body (inkl. `fieldErrors` aus dem `GlobalExceptionHandler`). UI mappt das auf nutzerverständliche Fehler.
- **Keine leeren `catch`-Blöcke.**
- **Keine technischen Fehlertexte (Stacktraces, Endpoints, Tokens) im UI.**
- **Mutationen müssen doppelte Submits verhindern** (Submit-Button mit `disabled` während Pending-State).
- Backend-DTOs und Frontend-Typen müssen synchron gehalten werden — bei Änderung der Java-DTOs immer auch die TS-Typen aktualisieren.

---

## ♿ Accessibility

- Semantisches HTML vor ARIA — Buttons lösen Aktionen aus, Links navigieren.
- MUI-Komponenten sind weitgehend a11y-tauglich; Custom-Wrapper dürfen das nicht kaputt machen.
- Sichtbare Fokus-Styles erhalten (Theme nicht so überschreiben, dass `:focus-visible` verschwindet).
- Tastaturbedienbarkeit für alle interaktiven Elemente.
- Formulare brauchen `<label>` (bei MUI: `<TextField label="…">`), verständliche Fehlermeldungen, passende `autocomplete`-Attribute.
- Bilder mit aussagekräftigem `alt` oder als dekorativ markieren.
- Farben dürfen nicht die einzige Informationsträger sein. Kontraste prüfen.

---

## ⚡ Performance

- Saubere Komponentenstruktur und lokaler State sparen die meisten Re-Renders.
- `useMemo`/`useCallback`/`React.memo` nur mit erkennbarem Nutzen. Keine pauschale Memoization.
- Selten genutzte oder große Routen können lazy geladen werden (`React.lazy` + `Suspense`) — Chunk-Grenzen fachlich sinnvoll schneiden.
- Listen mit stabilen IDs als Key — keine Array-Indizes bei veränderlichen Listen.
- Bundle-Auswirkung neuer Dependencies vor Hinzunahme prüfen (`vite build` zeigt Chunk-Größen).

---

## 📋 Formulare

- Clientseitige Validierung ist Nutzerführung, kein Ersatz für Server-Validierung (Spring `@Valid` ist die Quelle der Wahrheit).
- Fehler feldnah anzeigen, Eingaben bei Validierungsfehlern erhalten.
- Submit-Buttons während laufender Mutation deaktivieren.
- Passende `type=…`-Attribute und `autocomplete` setzen.
- MUI: `<TextField error={…} helperText={…}>` für Feld-Validierungsfehler. Aus `ApiError.body.fieldErrors` mappen.

---

## 🧭 Routing

- Routen sind flach (`/dashboard`, `/settings`, `/tools/...`). Keine verschachtelten Routen ohne Not.
- Routenkomponenten bleiben schlank — Datenladen und Komposition in Sub-Komponenten oder Hooks.
- Lade- und Fehlerzustände auf Routenebene behandeln, wenn dort geladen wird.
- URL-Parameter validieren oder defensiv interpretieren (`Number.parseInt(id, 10)` + Range-Check, nicht naked `+id`).
- React Router läuft mit Browser-History — der serverseitige SPA-Fallback (Spring `SpaForwardingController`) sorgt dafür, dass Direktaufrufe von Sub-URLs funktionieren.

---

## 🧪 Tests

- Neue oder geänderte Logik braucht Tests (Vitest + React Testing Library).
- Verhalten testen, nicht Implementierungsdetails — Tests sollen aus Nutzerperspektive lesbar sein.
- Kritische UI-Zustände abdecken: Loading, Error, Empty, Success, Disabled.
- Mocks realistisch und klein halten. Snapshot-Tests nur, wenn sie wirklich Stabilität messen.
- API-Mocks: bevorzugt `fetch` über `vi.spyOn(global, 'fetch')` oder `msw` — keine ungetypten Mock-Objekte.

---

## ❌ Verbotene Muster

- `any` ohne zwingende Begründung
- Type-Assertions zur Unterdrückung echter Modellfehler
- Non-Null-Assertions aus Bequemlichkeit
- Business-Logik tief im JSX
- Unnötiger globaler State
- Leere `catch`-Blöcke
- Index-Keys für dynamische Listen
- Unzugängliche Custom-Controls
- Neue Dependencies aus Bequemlichkeit
- Secrets im Client-Bundle
- `useEffect` für reine Datenableitung
- `dangerouslySetInnerHTML` ohne dokumentierten, geprüften Grund
- Große Refactorings ohne Aufgabenbezug

---

## 🔍 ESLint / A11y-Gate

**ESLint ist verbindlich** und muss vor jedem Push grün sein.

```bash
cd frontend && npm run lint   # ESLint auf src/
```

**Konfiguration:** [`eslint.config.js`](frontend/eslint.config.js) (flat config, ESLint 10+)
- `typescript-eslint` (recommended): TypeScript-Korrektheit, kein `any`
- `eslint-plugin-react` (recommended + jsx-runtime): React-Regeln
- `eslint-plugin-react-hooks` (recommended): Hooks-Regeln, `exhaustive-deps`
- `eslint-plugin-jsx-a11y` (recommended): Accessibility-Regeln

**Bewusst deaktiviert:**
- `react-hooks/set-state-in-effect`: Experimentelle Regel in react-hooks v7, die `useEffect(() => { void load(); }, [])` ablehnt — Standard-Datenfetch-Pattern in diesem Projekt.
- `jsx-a11y/no-autofocus`: `autoFocus` auf Modal-Dialog-Inputs ist ARIA-konform; pauschales Verbot erzeugt A11y-Rückschritt.

**Neue `eslint-disable`-Kommentare** im Produktivcode müssen im PR begründet werden.

**Pflichtchecks vor Push (Frontend):**

```bash
cd frontend && npm run build    # TypeScript + Vite
cd frontend && npm run lint     # ESLint + jsx-a11y
cd frontend && npm test         # Vitest
```

---

## 🔗 Weiterführende Docs

- [CLAUDE.md](CLAUDE.md) — Projekt-Übersicht
- [CLAUDE-java.md](CLAUDE-java.md) — Backend-Pendant
- [CLAUDE-security.md](CLAUDE-security.md) — XSS, Storage, Secrets, OIDC
- [CLAUDE-workflow.md](CLAUDE-workflow.md) — 9-Schritte-Workflow + Pflichtchecks
