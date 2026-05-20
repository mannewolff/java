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
  @Query("SELECT b FROM BookEntity b WHERE b.author = :author")
  List<BookEntity> findByAuthor(@Param("author") String author);
  ```
- Beispiel verboten:
  ```java
  entityManager.createQuery("SELECT b FROM BookEntity b WHERE b.author = '" + author + "'");  // ❌
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
