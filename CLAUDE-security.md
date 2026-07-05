# CLAUDE-security.md — Security & Compliance

Verbindliche Sicherheits-Regeln für Spring-Boot-Backend und React-Frontend. Diese Datei hat in Konflikten Vorrang vor Komfort- oder Performance-Erwägungen.

---

## 🔒 Grundprinzipien (nicht verhandelbar)

1. **Keine Secrets im Frontend.** Nichts mit `VITE_*`-Prefix darf vertraulich sein.
2. **Keine Secrets im Repo.** `.env`, `application-*.yml` mit echten Passwörtern, Keys oder Tokens sind gitignored — `application.yml` enthält ausschließlich Defaults mit Platzhaltern (`${VAR:default}`).
3. **Server-seitig autorisieren.** Clientseitige Checks sind UX, keine Sicherheit.
4. **Validieren statt vertrauen.** Jeder Wert aus Header/Body/Query/Path ist untrusted, bis er via Bean Validation oder Type-Guard gegengeprüft wurde.
5. **Prepared Statements / Parameter-Bindung überall.** Keine Ausnahme.
6. **Generische Fehler nach außen.** Stacktraces, SQL-Texte, Klassenpfade bleiben intern. `GlobalExceptionHandler` antwortet mit strukturierten, knappen Fehler-DTOs.

---

## 🔐 Frontend-Security

### Secrets & Tokens

- **Niemals** echte Geheimnisse als `VITE_*`-Variable. Alles dort landet im Client-Bundle.
- Falls Auth in dieses Projekt einzieht: Token im `HttpOnly`-Cookie speichern (vom Spring-Backend gesetzt), nicht in `localStorage`. `localStorage` ist nur dann akzeptabel, wenn ausdrücklich besprochen und das XSS-Risiko anders kontrolliert wird.
- Token-Expiry serverseitig prüfen; UI darf darauf vertrauen, dass das Backend abgelaufene Tokens ablehnt.

### OIDC-Token-Speicherung — dokumentierte Sicherheitsentscheidung

**Ist-Stand:** Tokens werden in `sessionStorage` gespeichert ([`oidcConfig.ts`](frontend/src/auth/oidcConfig.ts)), nicht in einem `HttpOnly`-Cookie.

**Begründung:**
Das Frontend ist ein PKCE-Public-Client (`toolbox-web`) ohne Backend-BFF (Backend-for-Frontend). Ein HttpOnly-Cookie erfordert ein BFF, das den OIDC-Code-Exchange serverseitig durchführt und das Cookie setzt — das ist für diese Single-User-Toolbox unverhältnismäßiger Infrastrukturaufwand.

`sessionStorage` statt `localStorage`:
- Tokens verschwinden beim Tab-Schließen (kein persistentes Risiko)
- Kein Cross-Tab-Leak (jeder Tab hat eigene `sessionStorage`)
- XSS-Risiko ist für React-Apps gering: automatisches Escaping aktiv, `dangerouslySetInnerHTML` verboten

**Restrisiko:** Jedes JavaScript auf derselben Origin kann `sessionStorage` lesen. Dieses Risiko wird durch folgende Gegenmaßnahmen kontrolliert:
- React escapt Textknoten automatisch (kein XSS durch normales Rendering)
- `dangerouslySetInnerHTML` ist projektweites Verbot (→ CLAUDE-security.md)
- Keine Service Workers (kein Worker-basierter XSS-Eskalationsvektor)
- Keycloak-Tokens sind kurzlebig (Standard-Expiry 5 min Keycloak-Default + Refresh-Token-Rotation)

**Mobile-Modus (`?pair=1`) — `localStorage` + Offline-Token:** Im Mobile-Pairing (#206) werden Tokens in `localStorage` gehalten und `offline_access` angefragt, damit das Handy bis zu 30 Tage (`offlineSessionIdleTimeout: 2592000`) ohne Neuanmeldung eingeloggt bleibt. Das ist bewusst — persistente Speicherung ist der Zweck. Das dadurch erhöhte Restrisiko (XSS, Backup-/Time-Machine-Snapshot, physischer Gerätezugriff → bis zu 30 Tage gültiger, den Logout überlebender Token) wird durch folgende Gegenmaßnahmen kontrolliert (#312):
- **Token-Revocation beim Logout:** `revokeTokensOnSignout: true` invalidiert Access- und Refresh-/Offline-Token beim `signoutRedirect` serverseitig am Revocation-Endpoint — ein Logout beendet den Offline-Zugang sofort, nicht erst nach 30 Tagen.
- **Refresh-Token-Rotation:** `revokeRefreshToken: true` + `refreshTokenMaxReuse: 0` in beiden Realms — jeder Refresh gibt einen neuen Token aus und invalidiert den alten. Ein geleakter Token wird beim nächsten legitimen Refresh unbrauchbar (Reuse-Detection); das Angriffsfenster schrumpft von 30 Tagen auf einen Refresh-Zyklus.
- **Offline-Idle-Timeout (30 Tage):** bewusst beibehalten — der Mobile-Zweck (#206) verlangt langlebige Sessions. Mit Rotation + Revocation ist das Restfenster eines unbemerkt geleakten, nie erneuerten Tokens auf den Idle-Timeout begrenzt; der Nutzer kann Sessions jederzeit über die Keycloak-Account-Console widerrufen. Bei Bedarf (mehr Sicherheit vor Komfort) hier reduzieren.

**Zukünftige Migration:** Wenn ein BFF eingeführt wird, ist auf HttpOnly-Cookie umzustellen. Bis dahin ist `sessionStorage` (Desktop) bzw. das abgesicherte `localStorage` (Mobile) die bewusst gewählte, akzeptierte Lösung.

### Bearer-Token-Copy-to-Clipboard in Settings

**Ist-Stand:** Die Settings-Seite zeigt den aktuellen Bearer-Token verkürzt und erlaubt Kopieren in die Zwischenablage ([`SettingsPage.tsx`](frontend/src/pages/SettingsPage.tsx)).

**Use-Case:** Developer-Workflow — Swagger UI (`/swagger-ui.html`) benötigt für authenticated Endpoints einen gültigen Bearer-Token zur manuellen API-Erkundung im Dev-Betrieb.

**Risiko und Akzeptanz:**
- Token liegt kurz in der System-Zwischenablage → sollte nach Nutzung gecleart werden
- Feature ist für eingeloggte User sichtbar (kein unauthenicated Zugriff)
- Kein Logging des Tokens, kein Server-seitiger Transfer

Dieses Feature ist **bewusst für Dev-Zwecke** vorgesehen. In Produktion ist es akzeptiert, da der Token nur dem eigenen User gehört. Wenn das Feature künftig entfernt werden soll, ist dafür ein separates Issue anzulegen.

### Input & XSS

- React escapt Textknoten automatisch — das schützt vor XSS, **solange** kein `dangerouslySetInnerHTML` verwendet wird.
- `dangerouslySetInnerHTML` ist verboten, außer der Eingang ist nachweislich bereinigt (DOMPurify o. ä.) **und** im Code begründet dokumentiert.
- Nutzer-Input nie ungeprüft in `href`-/`src`-Attribute schreiben (Schema-Whitelist: `https:`, `mailto:` — kein `javascript:`).

### Autorisierung in der UI

- UI-Elemente, die nur Eingeloggte sehen sollen, prüfen Auth-Status — **diese Checks sind kein Ersatz** für die serverseitige Prüfung in Spring.
- Defensiv: alles, was sensitive Daten oder Aktionen ermöglicht, hängt nicht allein an einem UI-State-Flag.

### Error Messages

- Keine technischen Fehlertexte (Stacktraces, Endpoints, Tokens) in der UI.
- Fehler aus `ApiError` (Wrapper in `frontend/src/api/client.ts`) sachlich anzeigen — Server-`message` kann gezeigt werden, Stacktrace-artige Inhalte nicht.

---

## 🍃 Spring-Boot-Backend-Security

### Konfiguration & Secrets

- DB-Passwort und sonstige Secrets ausschließlich über Umgebungsvariablen (`DB_PASSWORD`, …) oder externe Konfiguration (`SPRING_CONFIG_ADDITIONAL_LOCATION`).
- `application.yml` und `application-docker.yml` enthalten ausschließlich Defaults im Format `${VAR:default}` — keine Klartext-Geheimnisse.
- `.env` und `.env.local` sind gitignored. `.env.example` ist die Vorlage und enthält **keine** echten Werte.
- Spring Boot Actuator-Endpunkte werden auf das Minimum reduziert. Erlaubt aktuell: `health`, `info`. Niemals `env`, `configprops`, `beans` oder `mappings` ohne Auth in Produktion exponieren.
- `management.endpoint.health.show-details: when-authorized` (oder `never`) — nie `always` in Produktion.

### Input-Validation

- Eingehende DTOs sind Records mit Jakarta-Validation-Constraints (`@NotBlank`, `@Size`, `@Email`, …). Controller-Parameter mit `@Valid` annotieren.
- `MethodArgumentNotValidException` wird vom `GlobalExceptionHandler` zu HTTP 400 mit `fieldErrors`-Map gemappt.
- Pfad- und Query-Parameter explizit typisieren und einschränken (`@PathVariable Long id`, `@Min(1)`).
- Niemals `String`-Parsing per `parseLong`/`parseInt` ohne `try` oder Range-Check, wenn der Wert direkt in Geschäftslogik geht.

### Datenbankzugriff (CRITICAL)

- **JPA/Hibernate mit Parameter-Bindung.** Spring-Data-Repositories nutzen Named-Parameters oder Methoden-Naming.
- **JPQL/Native-SQL** ausschließlich mit `@Param`-gebundenen Parametern. **Niemals** Benutzereingaben in Query-Strings konkatenieren.
- Beispiel zulässig:
  ```java
  @Query("SELECT w FROM WidgetEntity w WHERE w.dashboardId = :dashboardId")
  List<WidgetEntity> findByDashboardId(@Param("dashboardId") Long dashboardId);
  ```
- Beispiel verboten:
  ```java
  entityManager.createQuery("SELECT w FROM WidgetEntity w WHERE w.dashboardId = " + id);  // ❌
  ```
- DDL-Änderungen ausschließlich über Flyway. `hibernate.ddl-auto=validate` in Produktion — niemals `update` oder `create-drop`.

### Datenbank-Berechtigungen

- DB-User der Anwendung erhält nur minimale notwendige Rechte (CRUD auf eigene Tabellen, kein `DROP`, kein `GRANT`, kein `CREATE USER`).
- Niemals `root` oder administrative Accounts für die Webanwendung.
- Getrennte Accounts und Passwörter pro Umgebung (dev/test/staging/prod).
- Migrationen können einen erweiterten Account brauchen — der ist getrennt vom Laufzeit-Account.

### Authentifizierung & Tokens (falls eingeführt)

- **Passwort-Hashing** über `BCryptPasswordEncoder` (Spring Security), Standard-Strength oder höher.
- **Token-Generierung** kryptographisch sicher (`SecureRandom`), in der DB ausschließlich der Hash gespeichert (`SHA-256` oder besser).
- **TTL** konfigurierbar über `@ConfigurationProperties`, Default eng (z. B. 8 h).
- Abgelaufene Tokens regelmäßig löschen (`@Scheduled` mit klar definiertem Cron).
- Brute-Force-Schutz: Failed-Login-Delay (zufällig 200–800 ms via `Thread.sleep` in einem dedizierten Use-Case) und Rate Limiting an der API-Grenze.
- Auth-Verhalten ist mit Spring-Security-Integration-Tests abgedeckt — nicht nur Happy-Path.

### CORS

- Im aktuellen Deployment ist Frontend und Backend auf derselben Origin — **CORS wird nicht benötigt**.
- Wenn das Frontend künftig auf eine andere Origin zieht: explizite `CorsConfiguration` mit konkreter Allow-Origin-Liste. Niemals `*` für Auth-belegte Endpunkte.

### HTTP-Header

- Spring Security-Defaults aktiviert lassen: `X-Content-Type-Options: nosniff`, `X-Frame-Options: DENY`, `Cache-Control: no-store` für Auth-Antworten.
- HSTS-Header in Produktion via Reverse-Proxy oder Spring-Security-Konfiguration.

### Error-Handling & Logging

- Globaler Handler via `@RestControllerAdvice` ([GlobalExceptionHandler.java](src/main/java/org/mwolff/api/common/GlobalExceptionHandler.java)). Nach außen: HTTP-Statuscode + generische Message + (bei Validation) `fieldErrors`. **Niemals** Stacktrace, SQL, interne Klassen.
- Logging über SLF4J/Logback. **Niemals** Secrets, Tokens, Passwörter, Klartext-PII, vollständige SQL-Queries mit Werten loggen.
- Logge nicht den kompletten Request-Body von Auth-Endpunkten.
- `printStackTrace()` ist verboten.

---

## 🐳 Container & Deployment

- `Dockerfile`: Multi-Stage-Build, finale Stage als JRE (kein JDK in Produktion). Kein `sudo`, keine zusätzlichen Tools außer wirklich nötigem (`curl` nur für Healthcheck).
- Container läuft idealerweise als Non-Root-User. Bei `eclipse-temurin:21-jre`: explizit Non-Root setzen, wenn das Image dies nicht schon tut.
- `docker-compose.yml`: keine Secrets als Klartext — über `.env` oder Compose-Secrets binden.
- Healthcheck nutzt `/actuator/health` mit minimalen Informationen.

---

## 🔍 Security-Checklist vor Commit

- [ ] Keine Secrets, Tokens, Passwörter, Hostnamen oder Cloud-Credentials im Diff.
- [ ] `.env`, `.env.local`, `application-local.yml` weiterhin gitignored — falls erst neu angelegt.
- [ ] Alle Datenbank-Queries mit Parameter-Bindung (`@Param`, JPA-Methoden-Naming oder Prepared Statements).
- [ ] Alle Controller-Parameter `@Valid`-annotiert, falls DTO; `@PathVariable`/`@RequestParam` typisiert.
- [ ] Keine `System.out.println`, `e.printStackTrace()`, `var_dump`-Äquivalente.
- [ ] Keine leeren `catch (Exception e) { }`-Blöcke.
- [ ] Frontend-Code referenziert keine internen Endpunkte/Pfade in Fehlermeldungen.
- [ ] `mvn verify` und `npm run build` grün.
- [ ] `dangerouslySetInnerHTML` nirgends ohne dokumentierten Grund.
- [ ] Actuator: keine sensiblen Endpunkte zusätzlich exponiert.

---

## 🚨 Rote Flaggen

- DB-Passwort, Token-Wert oder API-Key in `application.yml`, `Dockerfile`, Code oder Logs.
- Direkter Zugriff auf `request.getParameter(...)` o. ä. ohne Validation.
- JPQL/SQL-String mit Konkatenation von Nutzereingaben.
- `hibernate.ddl-auto=update` oder `create-drop` außerhalb von Tests.
- `management.endpoints.web.exposure.include: "*"` oder ungeschützter `/actuator/env`.
- Passwort-Hashing ohne BCrypt/Argon2 (z. B. selbstgebastelte MD5/SHA-Lösungen).
- Token-Vergleich mit `String.equals` auf Plain-Token statt Hash-Vergleich.
- React-Code mit `dangerouslySetInnerHTML={{ __html: userInput }}`.
- CORS `*` auf Auth-belegten Endpunkten.

---

## 🔗 Weiterführende Docs

- [CLAUDE.md](CLAUDE.md) — Projekt-Übersicht
- [CLAUDE-java.md](CLAUDE-java.md) — JPA, Spring-Designregeln
- [CLAUDE-react.md](CLAUDE-react.md) — XSS, Storage, Frontend-Patterns
- [CLAUDE-workflow.md](CLAUDE-workflow.md) — Pflichtchecks vor Push
- [GlobalExceptionHandler.java](src/main/java/org/mwolff/api/common/GlobalExceptionHandler.java) — zentrale Fehler-Mappings
- [application.yml](src/main/resources/application.yml) — Konfiguration mit Env-Vars
