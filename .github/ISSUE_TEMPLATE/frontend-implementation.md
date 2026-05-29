---
name: "Implementierung (Frontend / React)"
about: "React/TypeScript-Implementations-Issue mit verbindlicher Definition of Done"
title: ""
labels: []
assignees: []
---

## Kontext

<!-- Warum diese Aufgabe gemacht wird. Vorgeschichte. Was vorher fehlt. -->

## Aufgabe

<!-- Was konkret zu tun ist: Komponenten, Pages, API-Client, Theme. TDD-/Test-Liste hier. -->

## Akzeptanzkriterium

<!-- Wie man verifiziert, dass die Aufgabe erledigt ist. Konkret, im Browser durchklickbar. -->

## Abhängigkeiten

<!-- Andere Issues, die zuerst fertig sein müssen, oder "Keine". -->

## Definition of Done (Pflicht)

- [ ] TypeScript `strict` ohne neue `any` / `@ts-ignore`
- [ ] Tests vor dem Code geschrieben (Vitest + React Testing Library)
- [ ] Kritische UI-Zustände abgedeckt: Loading, Error, Empty, Success, Disabled
- [ ] Keine Snapshot-Tests ohne Mehrwert; Verhaltens- statt Implementierungs-Tests
- [ ] MUI-Konventionen + Theme statt Inline-Styles / Hardcoded-Farben
- [ ] Accessibility: Labels, Fokus, Tastaturbedienung geprüft
- [ ] Kein XSS-Risiko (kein ungesichertes `dangerouslySetInnerHTML`)
- [ ] Keine Secrets / Tokens im Bundle oder in `console.log`
- [ ] `cd frontend && npm run build` grün (`tsc -b && vite build`)
- [ ] Feature im Browser durchgeklickt (golden path + Edge-Cases)
- [ ] `/code-review` vor dem Push gelaufen, P1-Findings behoben (externes Zweit-Modell-Review optional)

<!-- Standards: siehe CLAUDE-react.md, CLAUDE-security.md, CLAUDE-workflow.md -->
