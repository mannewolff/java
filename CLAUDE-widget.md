# CLAUDE-widget.md — Dashboard-Widgets

Regeln und Vertrag für alle Dashboard-Widgets. Gilt für jedes Widget unter
`frontend/src/pages/dashboard/widgets/` und ist verbindlich, wenn ein neues Widget
hinzugefügt oder ein bestehendes geändert wird. Bei Sicherheitsfragen gilt
[CLAUDE-security.md](CLAUDE-security.md), bei React-Allgemeinem
[CLAUDE-react.md](CLAUDE-react.md).

---

## 🧩 Was ist ein Widget?

Eine Kachel auf dem Dashboard-Grid (react-grid-layout). Jedes Widget ist eine eigene
Komponente in `widgets/` und wird in `DashboardPage.renderWidgetBody` über ein
`switch (widget.type)` gerendert. Der Widget-Typ ist ein Enum, das Backend und Frontend
teilen.

Aktuelle Typen: `TEXTBOX`, `KPI`, `PLOT`, `KANBAN_LIST`, `IMAGE`, `DIVIDER`.

Der `IMAGE`-Typ referenziert ein im Backend gespeichertes Bild über seine `imageId`
(siehe Modul `image/`, #181) statt Base64 in der Widget-`config` abzulegen.

---

## 📜 Props-Vertrag (Pflicht für jedes Widget)

```ts
interface Props {
  widget: WidgetDto;                 // enthält u. a. config (opaker JSON-String)
  onChange: (next: WidgetDto) => void; // persistiert eine geänderte Config
  onDelete: () => void;                // entfernt das Widget aus dem Dashboard
  readOnly?: boolean;                  // default false
}
```

- `readOnly` wird von `DashboardPage` als `readOnly={!editMode}` gesetzt. **Lese-Modus**
  (Dashboard-Ansicht) = `true`, **Edit-Modus** = `false`.
- Default `false` ist Pflicht — die Unit-Tests rendern Widgets ohne `readOnly` und
  erwarten Edit-Verhalten.
- Einzelne Widgets dürfen zusätzliche optionale Props haben (z. B. `WidgetTextbox`
  meldet via `onContentHeight` seine natürliche Höhe), aber niemals weniger als den
  Vertrag oben.

---

## 🗄️ Config: opaker JSON-String + sichere Defaults

- `widget.config` ist ein **String** (JSON). Das Backend speichert ihn unverändert —
  **keine Schema-Migration** nötig, wenn sich Config-Felder ändern.
- Jedes Widget hat eine `parseConfig(raw: string)`-Funktion, die:
  - im `try` defensiv jedes Feld typprüft und mit einem Default belegt,
  - im `catch` eine vollständige Default-Config zurückgibt.
- **Rückwärtskompatibilität ist Pflicht:** Ältere gespeicherte Configs ohne neue Felder
  müssen weiterhin korrekt rendern. Niemals davon ausgehen, dass ein Feld existiert.
- Persistiert wird über `onChange({ ...widget, config: JSON.stringify(next) })`.

---

## 🎨 Darstellung: Rahmen + Hintergrundfarbe (alle Widgets)

Gemeinsame Logik in [`widgets/widgetSurface.ts`](frontend/src/pages/dashboard/widgets/widgetSurface.ts).
**Jedes** Widget nutzt sie — auch neue.

### Verhalten

| Modus | Rahmen | Hintergrund |
|---|---|---|
| Edit (`readOnly=false`) | immer `outlined` (Kachelgrenzen sichtbar) | Default |
| Lese, ohne Config | kein Rahmen (`elevation`, 0) | `transparent` |
| Lese, `showBorder: true` | `outlined` (MUI `divider`) | siehe `backgroundColor` |
| Lese, `backgroundColor` gesetzt | siehe `showBorder` | gesetzte Farbe |

### Integration (3 Schritte)

1. **Config-Typ + parseConfig:** zwei Felder über `parseSurfaceConfig` einlesen:
   ```ts
   interface MyConfig { /* … */ showBorder: boolean; backgroundColor?: string; }
   // im try:  return { …, ...parseSurfaceConfig(parsed) };
   // im catch: return { …, showBorder: false };
   ```
2. **Paper:** Variante/Elevation/sx aus `widgetSurface` ziehen:
   ```ts
   const surface = widgetSurface(readOnly, config);
   <Paper variant={surface.variant} elevation={surface.elevation}
          sx={{ /* widget-eigenes */ ...surface.sx }} />
   ```
3. **Drawer:** am Ende (vor Abbrechen/Übernehmen) den Abschnitt **„Darstellung"**:
   - `<Divider textAlign="left">Darstellung</Divider>`
   - `Switch` mit Label **„Rahmen anzeigen"** (`draftShowBorder`)
   - `TextField` mit Label **„Hintergrundfarbe (leer = transparent)"** (`draftBackgroundColor`)
   - In `handleApply`: leeren String als „kein Feld" behandeln —
     `...(draft.trim() !== '' ? { backgroundColor: draft.trim() } : {})`.

> ⚠️ Die Wirkung ist **nur im Lese-Modus** sichtbar. Im Edit-Modus bleibt die Kachel
> bewusst `outlined`, damit der User die Grenzen zum Ziehen/Resizen sieht.

---

## ✏️ Edit-Drawer-Konvention

- Edit-/Delete-`IconButton` nur bei `!readOnly` rendern (`aria-label` „<Widget> bearbeiten"
  / „<Widget> löschen").
- Konfiguration in einem `Drawer anchor="right"`; Breite aus
  [`drawerConstants.ts`](frontend/src/pages/dashboard/widgets/drawerConstants.ts)
  (`CONFIG_DRAWER_WIDTH`), oben ein `<Toolbar />`-Spacer.
- Draft-State pro Feld; beim Öffnen aus der aktuellen Config befüllen
  (`useEffect([open, …])`). „Abbrechen" verwirft, „Übernehmen" ruft `onChange`.

---

## 🖱️ react-grid-layout-Falle: Drag-Guard

Interaktive Elemente in der Kachel (Buttons, Links, Inputs) brauchen
`onMouseDown={(e) => e.stopPropagation()}`, sonst startet react-grid-layout beim
Mausklick einen Drag statt das Element zu bedienen. Gilt im Edit-Modus für alle
Klickziele innerhalb des Grid-Items.

---

## ➕ Neuen Widget-Typ hinzufügen — Checkliste

1. **Backend-Enum:** `WidgetType` in
   `src/main/java/org/mwolff/api/dashboard/domain/WidgetType.java` ergänzen
   (`@Enumerated(EnumType.STRING)` — Reihenfolge egal, Name zählt).
2. **Frontend-Typ:** `WidgetType` in
   [`frontend/src/api/dashboard.ts`](frontend/src/api/dashboard.ts) ergänzen.
3. **Defaults:** in
   [`widgetDefaults.ts`](frontend/src/pages/dashboard/widgetDefaults.ts)
   `WIDGET_DEFAULTS` (Grid-Größe) und `WIDGET_INITIAL_CONFIG` (Start-JSON) ergänzen.
4. **Palette:** Eintrag in `PALETTE_ENTRIES` in
   [`WidgetPalette.tsx`](frontend/src/pages/dashboard/WidgetPalette.tsx)
   (Label + MUI-Icon).
5. **Render-Switch:** `case` in `DashboardPage.renderWidgetBody` mit
   `readOnly={!editMode}`.
6. **Komponente:** neue Datei in `widgets/`, die den Props-Vertrag, das Config-Muster
   und den Darstellung-Abschnitt oben erfüllt.
7. **Tests:** Vitest + React Testing Library (siehe unten).

---

## 🧪 Test-Hinweise

- Pflicht-Abdeckung pro Widget: Default-Render, Lese-vs-Edit-Modus, parseConfig-Defaults
  bei invalider Config, Drawer speichert Config (inkl. Darstellung-Felder).
- **MUI `Select`:** erst `getByRole('combobox', { name })` klicken, dann
  `getByRole('option', { name })`.
- **MUI `Switch`:** Rolle ist `checkbox` —
  `getByRole('checkbox', { name: 'Rahmen anzeigen' })`.
- **jsdom-Falle:** `toHaveStyle({ backgroundColor: 'transparent' })` schlägt fehl
  (jsdom normalisiert zu `rgba(0, 0, 0, 0)`). Stattdessen Variante/Klasse prüfen
  (`not.toHaveClass('MuiPaper-outlined')`) oder den konkreten Farbwert.
- **recharts** rendert in jsdom kein SVG zuverlässig — reine Helfer (z. B. `overlayValue`)
  als Unit-Tests prüfen, nicht das gezeichnete Chart.

---

**TL;DR:** Jedes Widget erfüllt den Props-Vertrag (`widget/onChange/onDelete/readOnly`),
parst seine Config defensiv mit Defaults, nutzt `widgetSurface` + den „Darstellung"-Drawer-
Abschnitt für Rahmen/Hintergrund (nur im Lese-Modus sichtbar), schützt Klickziele mit
`onMouseDown`-stopPropagation und ist mit Vitest/RTL getestet. Neuer Typ = 7-Punkte-Checkliste.
