# Best Practices der KI-Programmierung

> Erfahrungswerte aus der praktischen Zusammenarbeit zwischen Mensch und KI an einem
> realen Software-Projekt (Java/Spring-Backend + React/TypeScript-Frontend). Festgehalten
> als Grundlage für spätere Artikel.

---

## Leitprinzip

**Die KI strukturiert, formuliert und implementiert. Der Mensch ordnet ein, gibt frei
und trägt die Verantwortung.**

Geschwindigkeit ist nicht der primäre Wert — **Klarheit und Kontrolle** sind es. Fast
alle folgenden Praktiken sind Ausprägungen dieses einen Satzes.

---

## 1. Kontrolle & Verantwortung (Mensch)

### 1.1 Aktiv überwachen und „ermahnen"
Die KI arbeitet schnell und wirkt selbstsicher — auch dann, wenn sie eine Regel gerade
übergeht. Der Mensch ist die Kontrollinstanz: mitlesen, einordnen, bei Abweichungen
sofort korrigieren. Eine deutliche Rüge im richtigen Moment ist kein Misstrauen, sondern
Teil des Verfahrens. Die KI hat kein verlässliches Eigeninteresse, den Prozess
einzuhalten — der Mensch erzwingt ihn.

> **Aus der Praxis:** Die KI begann einmal ein Issue umzusetzen, das nur im Backlog lag
> (nicht freigegeben). Erst die ausdrückliche Korrektur stellte klar: *Eine Entscheidung
> im Chat ist keine Freigabe.* Diese Lektion wurde dauerhaft festgehalten.

### 1.2 Der Mensch ist verantwortlich, dass die KI den Workflow einhält
Nicht „die KI hält sich an den Prozess", sondern „**ich sorge dafür**, dass sie es tut".
Diese Haltung verhindert, dass man sich auf Zusagen der KI verlässt, die im nächsten
Schritt schon wieder vergessen sind. Verbindliche Regeln gehören in eine projektweite
Datei (z. B. `CLAUDE.md` / Guides), die die KI zu Beginn jeder Session liest — nicht in
den flüchtigen Chat-Verlauf.

### 1.3 Explizite Trigger-Phrasen, niemals Automatik
Heikle, nach außen wirkende Schritte passieren **nur** auf ein klares Stichwort in der
**aktuellen** Nachricht:

- `GO` / `los` → Implementierung beginnen
- `push main` → auf den Testserver/Remote pushen
- `merge production` → Release nach Produktion

Eine frühere Freigabe gilt **nicht** für den nächsten Commit. Jeder Commit-Batch braucht
eine eigene Freigabe. „Ich pushe schnell, der Mensch merkt's schon" ist ein verbotener
Gedanke.

### 1.4 Orchestrierung über getrennte Sessions — Mensch als Taktgeber
Es lohnt sich, **Phasen auf verschiedene Sessions (oder Modelle) aufzuteilen** statt alles
in einem Strang zu erledigen:

- Eine Session dient nur dem **Anforderungs-Grooming**: eine Anforderung nach der anderen
  formulieren, ins Backlog wachsen lassen, später erneut ansehen und bewusst nach **Ready**
  schieben.
- Eine andere Session **implementiert** das, was in Ready liegt.

Das trennt zwei sehr unterschiedliche Denkmodi (Was wollen wir? vs. Wie bauen wir es?) und
hält beide fokussiert.

**Warum kein vollautomatischer Durchlauf?** Theoretisch könnte ein Modell Ready-Items
selbstständig abgreifen und losbauen. In der Praxis scheitert das daran, dass das
implementierende Modell **berechtigte Rückfragen** hat — Designentscheidungen, die nur der
Mensch treffen kann (siehe §4). Ein „Ready → fertig"-Fließband ohne Mensch würde an genau
diesen Fragen hängenbleiben oder, schlimmer, sie raten.

**Das praktikable Optimum** ist deshalb bewusst halb-manuell: Der Mensch ist beim Start
dabei, **klärt die offenen Fragen up-front**, gibt frei — und tritt dann zurück
(„Kaffee holen"), während die KI die geklärte, zusammenhängende Welle abarbeitet. Danach
prüft der Mensch das Ergebnis (§2). Anwesend an den Entscheidungspunkten, abwesend bei der
Fleißarbeit.

---

## 2. Gestuftes Testen vor jedem Release

Das vielleicht wichtigste Sicherheitsnetz: **nichts wird durchgereicht, ohne es auf der
jeweiligen Stufe selbst geprüft zu haben.**

1. **Lokal prüfen, was die KI eingecheckt hat** — nicht blind dem „Tests grün"-Bericht
   vertrauen, sondern das Feature lokal durchklicken (Dev-Server, echte Eingaben,
   Edge-Cases).
2. **Erst danach `push main`** → Deploy auf den Testserver.
3. **Auf dem Testserver erneut testen** — in einer produktionsnahen Umgebung (echte DB,
   echtes Auth, echte Assets).
4. **Erst dann `merge production`** → Release, ggf. nochmal auf Produktion gegentesten.

Jede Stufe ist eine eigene Gelegenheit, einen Fehler zu fangen, bevor er teurer wird.
Auch eine vermeintlich triviale CSS-Änderung kann auf dem Testserver Schaden anrichten —
ein Push ist ein **Release-Schritt**, kein Implementierungsdetail.

> **Aus der Praxis:** Ein interaktives Resize-Feature hatte grüne Unit-Tests, funktionierte
> im echten Browser aber nicht (Pointer-Events trafen die winzigen Greifpunkte nicht).
> Nur der **manuelle Test durch den Menschen** deckte das auf. „Grün" heißt nicht „richtig".

---

## 3. Issues als Quelle der Wahrheit

### 3.1 Plan → kleinteilige, selbsterklärende Issues
Bevor implementiert wird, wird der Plan in **kleine** Issues überführt — eines pro
logischer Einheit, jedes mit Kontext, Aufgabe, Akzeptanzkriterium und Abhängigkeiten.
Ein Issue muss **ohne** Chat-Verlauf verständlich sein. Das Issue ist die Quelle der
Wahrheit für die Implementierung, nicht der Chat.

### 3.2 Eine sichtbare Freigabe-Spalte (Kanban „Ready")
Ein Board mit einer **Ready**-Spalte, die nur der Mensch befüllt, formalisiert das „GO".
„Implementiere alles in Ready" ist eine saubere, eindeutige Pauschal-Freigabe. Wichtig:
Eine Diskussion im Chat verschiebt **nichts** nach Ready — die Freigabe ist ein
bewusster, sichtbarer Akt.

### 3.3 Out-of-Scope-Funde parken, nicht einbauen
Fällt während einer Aufgabe etwas anderes auf (Bug, Verbesserung), wird es als neues
Backlog-Issue notiert — nicht spontan in den laufenden Change gepackt. Das hält Changes
fokussiert und reviewbar.

> **Aus der Praxis:** Beim Bau der Bild-Galerie fiel auf, dass Thumbnails als Vollbilder
> geladen wurden. Statt das sofort umzubauen, entstand ein separates Issue für einen
> server-seitigen Thumbnail-Endpoint — und wurde später als eigene Einheit umgesetzt.

---

## 4. Offene Fragen *vor* dem Code klären

Bei Designentscheidungen mit echten Alternativen wird **vor** der Implementierung
gefragt, nicht geraten. Eine kurze Klärung spart eine teure Neufassung.

> **Aus der Praxis:** Zwei Issues forderten denselben API-Endpoint in
> unterschiedlicher Form. Eine gezielte Rückfrage („eine Superset-Antwort einmal bauen,
> oder zwei getrennte?") legte die Architektur sauber fest, bevor eine Zeile entstand —
> statt erst das eine zu bauen und dann das andere darüber zu refactoren.

Regel: **Bei Unklarheit nachfragen, nicht den nächsten Schritt erfinden.** Wenn ein
Satz (z. B. per Spracherkennung) abbricht oder Lücken hat: nachfragen.

---

## 5. Qualität automatisch absichern

### 5.1 Self-Review vor jedem Push
Vor jedem Push läuft ein automatisierter Review über die **gesamte** ungepushte
Commit-Reihe (nicht pro Issue) und klassifiziert Funde nach Schwere (P1 blockt den Push,
P2/P3 landen im Backlog). Das fängt genau die unscheinbaren Verstöße ab, die
Test-Suiten nicht abdecken (Namensregeln, Schicht-Drift, fehlende Validierung). Der
Self-Review **ersetzt** den menschlichen Review nicht, er ergänzt ihn.

### 5.2 Pflichtchecks ohne Ausnahme
Vor Abschluss laufen die vollen Checks (Build, Tests, Coverage, statische Analyse;
Frontend: Lint + Tests + Build). Kein Umgehen von Hooks (`--no-verify`), keine
„Coverage ist bei 98 %, das reicht"-Abkürzungen. Wenn ein Check nicht ausführbar ist
(kein Docker-Daemon o. ä.), wird das **ehrlich vermerkt** und nicht als „passt schon"
verkauft.

### 5.3 Tests als Reproduktions- und Regressionsnachweis
Bei einem Bug zuerst einen Test schreiben, der den Fehler **reproduziert** — und das
echte Verhalten möglichst getreu nachstellt. Der Bug gilt erst als gefixt, wenn der
Test grün ist und bleibt.

> **Aus der Praxis:** Der ursprüngliche Test feuerte Events direkt auf das Element und
> war deshalb grün, obwohl der echte Browser sich anders verhielt. Der Fix bestand auch
> darin, den Test *realistischer* zu machen (Events global statt am Element) — so wäre
> der Bug von Anfang an aufgefallen.

### 5.4 Coverage-Ehrlichkeit
Praktisch unerreichbare Codepfade (z. B. eine IO-Exception bei In-Memory-Streams) werden
**mit Begründung explizit ausgeschlossen**, statt mit künstlichen Tests Coverage
vorzutäuschen. Und: Kappt ein Stück Code etwas (Top-N, kein Retry, Sampling), wird das
**sichtbar gemacht** — stilles Abschneiden liest sich wie „alles abgedeckt", obwohl es
das nicht ist.

---

## 6. Saubere Git- & Release-Hygiene

- **Commits lokal, niemals automatisch pushen.** Ein Commit pro logischer Einheit, mit
  klarer, erklärender Message.
- **Issue-Lifecycle an den Branch koppeln:** Auf dem Integrations-Branch nur `Refs #N`
  referenzieren — `Closes/Fixes #N` schließt das Issue verfrüht. Geschlossen wird erst
  beim Release nach Produktion (oder manuell durch den Menschen).
- **Status-Übergänge gehören dem Menschen:** „Done" / Schließen entscheidet der Mensch,
  nicht die Automatik und nicht die KI.

> **Aus der Praxis:** Einmal schlossen `Closes #N`-Commits Issues schon beim Push auf den
> Testserver — die Projekt-Automatik schob sie nach „Done", obwohl sie noch gar nicht in
> Produktion waren. Seitdem gilt strikt `Refs #N`.

---

## 7. Constraints lenken das Design — nicht umgehen

Architektur- und Sicherheitsregeln (z. B. „Module dürfen sich nicht gegenseitig
referenzieren", Parameter-Bindung in Queries, kein `dangerouslySetInnerHTML`) sind kein
Hindernis, sondern eine Leitplanke. Statt sie zu umgehen, wird die Lösung an ihnen
ausgerichtet.

> **Aus der Praxis:** Eine Bild-Skalierung sollte ursprünglich einen bestehenden
> Python-Dienst nutzen. Weil die Architekturregel diese Modul-Abhängigkeit verbietet,
> wurde die Skalierung stattdessen JVM-seitig gelöst — die Regel hat das Design
> verbessert, nicht behindert.

**Prioritäten bei Zielkonflikten** (von hoch nach niedrig): Sicherheit → Korrektheit →
Datenintegrität → Accessibility → Wartbarkeit → Testbarkeit → Performance → visuelle
Präferenz → Bequemlichkeit der Implementierung. Keine kurzfristige Bequemlichkeit
rechtfertigt unsicheren oder schwer wartbaren Code.

---

## 8. Funktional korrekt ≠ richtig

Eine KI optimiert auf „erfüllt die Spezifikation / Tests sind grün". Ob das Ergebnis
auch dem **eigentlichen Bedürfnis** entspricht, entscheidet der Mensch. Genau dafür ist
der Mensch im Loop unverzichtbar.

> **Aus der Praxis:** Ein Resize-Vorschaubild war technisch korrekt (richtige
> Ausgabemaße), zeigte aber ein Overlay statt das schrumpfende Bild — nicht das, was der
> Nutzer wollte. Kein Test hätte das gefunden; nur das menschliche Auge.

---

## 9. Gedächtnis über Sessions hinweg

KI-Sessions sind vergesslich. Ein **persistentes Memo** (letzter Stand, offene Punkte,
hart erkämpfte Konventionen, projektspezifische IDs/Befehle) am Sessionende spart beim
nächsten Mal viel Anlauf und verhindert Wiederholungsfehler. Zu Sessionbeginn liest die
KI dieses Memo **und** die verbindlichen Regeln, damit Mensch und KI dasselbe Verständnis
haben.

---

## TL;DR

1. Mensch kontrolliert, ordnet ein, gibt frei — und ist verantwortlich, dass der Prozess
   eingehalten wird.
2. Phasen über getrennte Sessions orchestrieren (Grooming ≠ Implementierung); Mensch ist
   an den Entscheidungspunkten dabei, klärt up-front, tritt dann zurück. Kein
   vollautomatischer Durchlauf — die Rückfragen brauchen einen Menschen.
3. Gestuft testen: lokal → Testserver → Produktion. „Grün" ≠ „richtig".
4. Issues (nicht der Chat) sind die Quelle der Wahrheit; Freigabe ist ein sichtbarer Akt.
5. Designfragen **vor** dem Code klären.
6. Automatische Qualität (Review, Build, Tests, Coverage) — ohne Abkürzungen, mit
   ehrlichem Reporting.
7. Saubere Git-/Release-Hygiene; Status-Übergänge gehören dem Menschen.
8. Regeln lenken das Design, statt umgangen zu werden.
9. Persistentes Gedächtnis über Sessions hinweg.
