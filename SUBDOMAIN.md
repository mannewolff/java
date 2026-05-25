# Memo: Docker-Setup auf Hostinger-Server

Stand: 22. Mai 2026

## Ausgangslage

Manne betreibt einen Hostinger-Server (Hostname `srv1014330.hstgr.cloud`, IP `72.60.131.171`)
mit mehreren Docker-Containern. Daneben gibt es einen Strato Managed Server für ca. 10
Webseiten (Wordpress, React+PHP), auf dem Docker nicht möglich ist (Strato bietet Docker
auf Managed Servern nicht an). Container-Workloads laufen daher ausschließlich auf
Hostinger.

Die Domain `mwolff.org` wird bei Strato verwaltet (DNS dort).

## Architektur auf dem Hostinger-Server

Es gibt zwei getrennte Docker-Compose-Stacks, die sich ein externes Netzwerk teilen.

### Stack 1: n8n-Stack (vermutlich unter `/root` oder ähnlich)

Enthält:

- **Traefik v3.1** als Reverse Proxy mit Let's Encrypt (TLS-Challenge)
- **n8n** unter `${SUBDOMAIN}.${DOMAIN_NAME}` (aktuell auf `hstgr.cloud`)
- **ollama** unter `ollama.${DOMAIN_NAME}` (aktuell auf `hstgr.cloud`)
- **open-webui** unter `${WEBUI_SUBDOMAIN}.${DOMAIN_NAME}` (aktuell auf `hstgr.cloud`)

Container-Namen: `root-traefik-1`, `root-n8n-1`, `ollama`, `open-webui`

### Stack 2: Toolbox-Stack (in `/root/opt/java`)

Enthält:

- **mariadb** (intern, nicht von außen erreichbar)
- **python-tools** (intern, Port 8000)
- **api** (Spring Boot Java-App, Port 8080, erreichbar unter `toolbox.mwolff.org`)

Source Code wird per Git auf den Server gezogen, Images werden direkt auf dem Server
gebaut (`build: .` und `build: ./python-tools` in der Compose-Datei).

Workflow für Updates:

```bash
cd /root/opt/java
git pull
docker compose up -d --build
```

### Netzwerk-Setup

Externes Docker-Netzwerk `web`, an dem Traefik und alle nach außen exponierten Container
hängen. MariaDB und python-tools hängen nur am internen Netzwerk `toolbox-internal` und
sind damit vom Internet abgeschottet.

Erstellt wurde das Netzwerk einmalig mit:

```bash
docker network create web
```

## Was schon umgestellt wurde

Die Toolbox läuft jetzt unter `toolbox.mwolff.org` mit gültigem Let's Encrypt Zertifikat.
Vorgehensweise war:

1. A-Record bei Strato angelegt: `toolbox.mwolff.org` → `72.60.131.171`
2. `.env` im Toolbox-Verzeichnis angepasst: `DOMAIN_NAME=mwolff.org` (NUR die Hauptdomain,
   das `toolbox.` wird in den Traefik-Labels davorgesetzt)
3. `docker compose down && docker compose up -d` im Toolbox-Verzeichnis
4. Traefik (aus dem n8n-Stack) hat das Zertifikat automatisch über die TLS-Challenge geholt

## Warum die Domain-Umstellung nötig war

Let's Encrypt hat ein Rate Limit von 50 neuen Zertifikaten pro Woche pro registrierter
Domain. Da `hstgr.cloud` von tausenden Hostinger-Kunden geteilt wird, ist dieses Limit
permanent ausgereizt. Eigene Domains wie `mwolff.org` haben ihr eigenes Rate Limit, das
in der Praxis nie erreicht wird.

## Was noch zu tun ist: n8n, ollama, open-webui auf mwolff.org umziehen

Aktuell laufen diese drei noch unter `*.hstgr.cloud`. Solange die bestehenden Zertifikate
gültig sind (Let's Encrypt: 90 Tage), passiert nichts. Sobald die Erneuerung ansteht,
könnte sie wegen des Rate Limits fehlschlagen. Daher ist eine Umstellung sinnvoll.

### Schritt 1: DNS-Records bei Strato anlegen

Für jede Subdomain einen A-Record bei Strato anlegen, der auf `72.60.131.171` zeigt:

- `n8n.mwolff.org` (oder welche Subdomain auch immer in `SUBDOMAIN` steht)
- `ollama.mwolff.org`
- `openwebui.mwolff.org` (oder welche Subdomain in `WEBUI_SUBDOMAIN` steht)

Alternative: Wildcard-Record `*.mwolff.org` → `72.60.131.171`. Achtung, wirkt sich dann
auf alle künftigen Subdomains aus, was unerwünscht sein kann, wenn `mwolff.org` für
andere Dienste (E-Mail, Webseiten auf anderen Servern) genutzt wird.

DNS-Propagation prüfen mit `dig <subdomain>.mwolff.org +short` vom lokalen Rechner.
Sollte die IP `72.60.131.171` zurückgeben.

### Schritt 2: .env des n8n-Stacks anpassen

In das Verzeichnis des n8n-Stacks wechseln (vermutlich `/root` oder ähnlich, da Volume-
Präfixe wie `root_n8n_data` darauf hindeuten). Suche notfalls mit:

```bash
find / -name "docker-compose.yml" -not -path "*/node_modules/*" 2>/dev/null
```

Dann die `.env` anpassen:

```
DOMAIN_NAME=mwolff.org
SUBDOMAIN=n8n
WEBUI_SUBDOMAIN=openwebui
SSL_EMAIL=<deine-email-für-letsencrypt>
GENERIC_TIMEZONE=Europe/Berlin
```

### Schritt 3: Auflösung prüfen

```bash
docker compose config | grep -i "host("
```

Sollte ausgeben:

```
rule: Host(`n8n.mwolff.org`)
rule: Host(`ollama.mwolff.org`)
rule: Host(`openwebui.mwolff.org`)
```

### Schritt 4: Stack neu starten

```bash
docker compose up -d
```

(Kein `down` nötig, Compose erkennt geänderte Labels und startet die betroffenen
Container neu.)

### Schritt 5: Logs beobachten

```bash
docker logs -f root-traefik-1 2>&1 | grep -i -E "acme|certificate|error"
```

Erfolgreich, wenn drei `Certificate obtained for domain ...` Einträge erscheinen.

### Schritt 6: Im Browser testen

- `https://n8n.mwolff.org`
- `https://ollama.mwolff.org`
- `https://openwebui.mwolff.org`

Jeweils mit grünem Schloss.

## Bekannte Stolpersteine

- **Variable `${DOMAIN_NAME}` fehlt oder ist leer**: Traefik versucht ein Zertifikat für
  `toolbox.` (mit Punkt am Ende) zu holen und scheitert mit `Domain name needs at least
  one dot`. Lösung: `.env` prüfen, `docker compose config` zur Verifikation nutzen.

- **Doppeltes Präfix**: Wenn `DOMAIN_NAME=toolbox.mwolff.org` (mit Subdomain) gesetzt
  wird statt `DOMAIN_NAME=mwolff.org`, wird daraus `toolbox.toolbox.mwolff.org`. Das
  `toolbox.` setzt die Compose-Datei selbst davor.

- **Rate Limit bei `hstgr.cloud`**: Let's Encrypt verweigert neue Zertifikate. Wechsel
  auf eigene Domain (`mwolff.org`) löst das.

- **DNS noch nicht propagiert**: Strato sagt bis zu 24 Stunden, praktisch meist 5 bis
  30 Minuten. Prüfen mit `dig` oder https://www.whatsmydns.net/

- **Port 8080 öffentlich**: In der Toolbox-Compose war ursprünglich `ports: "8080:8080"`
  drin. Das wurde entfernt, weil Traefik den Traffic ohnehin intern routet und es sonst
  einen ungeschützten Zugang ohne TLS gegeben hätte.

- **Traefik-Volume bei mehreren Stacks**: Es existieren auf dem Server mehrere
  `*_traefik_data` Volumes (`root_traefik_data`, `openwebui_traefik_data`,
  `traefik_data`), Überbleibsel von früheren Stack-Konfigurationen. Aktuell relevant
  ist nur das Volume, das `root-traefik-1` mountet (vermutlich `root_traefik_data`).

## Nützliche Befehle

```bash
# Welche Container laufen?
docker ps

# Logs eines Containers
docker logs -f <container-name>

# Logs der letzten 10 Minuten
docker logs --since 10m <container-name>

# Auflösung der Compose-Variablen prüfen
docker compose config | grep -i "host("

# Netzwerk-Inspektion
docker network inspect web

# DNS-Check vom lokalen Rechner
dig <subdomain>.mwolff.org +short

# Status aller Toolbox-Container
cd /root/opt/java && docker compose ps
```

## Offene Punkte / spätere Verbesserungen

- Eigene Domain für n8n, ollama, open-webui (siehe oben).
- Migration auf Image-basiertes Deployment (Build lokal oder in CI, Images in
  Registry pushen, Server zieht nur fertige Images). Aktuell wird auf dem Server
  selbst gebaut, was bei größeren Apps Ressourcen kostet.
- Traefik-Dashboard absichern (aktuell `api.insecure=true` in der Konfiguration des
  n8n-Stacks, was vermutlich noch so ist).
- Aufräumen der ungenutzten `*_traefik_data` Volumes (nur das aktuell gemountete
  behalten, andere mit `docker volume rm` entfernen, NACHDEM klar ist, welches benutzt
  wird)
