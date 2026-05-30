---
name: "Implementierung (Backend / Java)"
about: "Java/Spring-Implementations-Issue mit verbindlicher Definition of Done"
title: ""
labels: []
assignees: []
---

## Kontext

<!-- Warum diese Aufgabe gemacht wird. Vorgeschichte. Was vorher fehlt. -->

## Aufgabe

<!-- Was konkret zu tun ist: Dateien, Klassen, Schicht. TDD-Liste (Tests zuerst) hier. -->

## Akzeptanzkriterium

<!-- Wie man verifiziert, dass die Aufgabe erledigt ist. Konkret, mess- oder ausführbar. -->

## Abhängigkeiten

<!-- Andere Issues, die zuerst fertig sein müssen, oder "Keine". -->

## Definition of Done (Pflicht)

- [ ] Architektur-Schicht klar zugeordnet (Domain / Application / Infrastructure / Web)
- [ ] Tests vor dem Code geschrieben (TDD)
- [ ] AssertJ statt Hamcrest, Given/When/Then-Struktur
- [ ] JaCoCo 100 % Line + Branch Coverage auf neuen Klassen
- [ ] PIT Mutation Score + Test Strength 100 % auf neuen Klassen
- [ ] ArchUnit-Regeln grün (keine Schicht-Verletzung)
- [ ] Spotless / Checkstyle / SpotBugs / PMD / Error Prone ohne Findings
- [ ] Keine Secrets im Diff
- [ ] Endpunkte mit `@PreAuthorize` o. explizit als öffentlich dokumentiert
- [ ] `@Valid` auf Controller-DTOs
- [ ] Keine Klartext-Logs sensibler Daten
- [ ] `mvn verify` grün (Tests, Coverage, Mutation, ArchUnit, Static Analysis)
- [ ] `/code-review` vor dem Push gelaufen, P1-Findings behoben (externes Zweit-Modell-Review optional)

<!-- Standards: siehe CLAUDE-java.md, CLAUDE-security.md, CLAUDE-workflow.md -->
