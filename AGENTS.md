# AGENTS.md

Dieses Projekt pflegt seine Engineering-Standards in **[CLAUDE.md](CLAUDE.md)** und
der dort verlinkten Guide-Familie. Diese Datei existiert nur als Einstiegspunkt für
Agenten, die den `AGENTS.md`-Standard lesen (Codex, Cursor, Aider u.a.) — sie hält
**keinen eigenen Inhalt**, um Drift zu vermeiden. Es gibt genau eine Quelle der
Wahrheit: CLAUDE.md.

## Verbindlicher Einstieg

Lies **[CLAUDE.md](CLAUDE.md)** und die dort verlinkten Sub-Guides:

| Guide | Fokus |
|---|---|
| [CLAUDE.md](CLAUDE.md) | Projekt-Übersicht, Stack, Projektstruktur, Pflichtchecks, Prioritäten |
| [CLAUDE-workflow.md](CLAUDE-workflow.md) | 9-Schritte-Workflow, Plan-Mode, Issues, Git, GO-Freigabe |
| [CLAUDE-java.md](CLAUDE-java.md) | Java 21, Spring Boot 3, TDD, Coverage, Mutationstests |
| [CLAUDE-react.md](CLAUDE-react.md) | React 18, Vite, TypeScript strict, MUI |
| [CLAUDE-widget.md](CLAUDE-widget.md) | Dashboard-Widgets: Props-Vertrag, Config, neuer Typ |
| [CLAUDE-security.md](CLAUDE-security.md) | Spring Security, JPA, Frontend-XSS, Secrets (hat in Sicherheitsfragen Vorrang) |

## Das Wichtigste in einem Satz

Java 21 + Spring Boot 3 (TDD-pflichtig, 100 % Coverage), React 18 + TypeScript strict + MUI.
Priorität: Sicherheit > Korrektheit > Komfort. Vor jedem Push `mvn verify` und
`npm run build` grün. Plan-Mode und GitHub-Issues sind verbindlich, Push nur auf
explizite Anweisung. Details in CLAUDE.md.
