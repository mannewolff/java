# Public Ingest API

Mit der öffentlichen Ingest-API können externe Programme (IoT-Sensoren, Cron-Skripte,
mobile Apps Dritter) Messwerte in deine Zeitreihen schreiben — ohne JWT-Login.

## Authentifizierung

Pro Programm einen langlebigen Ingest-Token erzeugen:

1. Im Web-UI: **Einstellungen → Ingest-Tokens → Neu**.
2. Name vergeben (z. B. `Raspberry Pi Wohnzimmer`).
3. Den Plaintext-Token (`tk_<64-hex>`) sofort kopieren — er wird **nur einmal** angezeigt.

Token zurückziehen geht jederzeit in derselben UI; widerrufene Tokens liefern ab sofort
`401`.

## Endpoint

```
POST /api/ingest
X-Ingest-Token: tk_<64-hex>
Content-Type: application/json

{
  "timeSeriesId": 42,
  "timestamp": "2026-05-27T12:00:00Z",
  "value": 78.5
}
```

Antwort `201 Created`:

```json
{ "id": 99, "timestamp": "2026-05-27T12:00:00Z", "value": 78.5 }
```

## Fehler

| Code | Bedeutung |
|---|---|
| 400 | Body-Validation fehlgeschlagen (fehlende Felder, scale > 6, `INTEGER`-Serie mit Nachkommastellen, ungültiges Datum) |
| 403 | Token fehlt oder ist ungültig/widerrufen |
| 404 | Zeitreihe gehört nicht zum Token-Besitzer oder existiert nicht |
| 429 | Rate-Limit überschritten (Standard: 60 Requests pro Minute pro Token) |

## Rate-Limit

Standard: **60 Requests pro Minute pro Token**, Fixed-Window. Konfigurierbar via
`toolbox.ingest.rate-limit.capacity` und `toolbox.ingest.rate-limit.window-millis` in
`application.yml`.

## curl-Beispiel

```bash
curl -X POST https://toolbox.mwolff.org/api/ingest \
  -H "X-Ingest-Token: tk_xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx" \
  -H "Content-Type: application/json" \
  -d '{"timeSeriesId": 42, "timestamp": "2026-05-27T12:00:00Z", "value": 78.5}'
```

## Python-Snippet

```python
import requests
from datetime import datetime, timezone

requests.post(
    "https://toolbox.mwolff.org/api/ingest",
    headers={"X-Ingest-Token": "tk_..."},
    json={
        "timeSeriesId": 42,
        "timestamp": datetime.now(timezone.utc).isoformat().replace("+00:00", "Z"),
        "value": 78.5,
    },
    timeout=10,
).raise_for_status()
```

## Bash-Snippet (mit `jq`)

```bash
NOW=$(date -u +"%Y-%m-%dT%H:%M:%SZ")
curl -fsSL -X POST https://toolbox.mwolff.org/api/ingest \
  -H "X-Ingest-Token: $TOOLBOX_TOKEN" \
  -H "Content-Type: application/json" \
  -d "$(jq -nc --arg t "$NOW" '{timeSeriesId: 42, timestamp: $t, value: 78.5}')"
```

## OpenAPI-Schema

Das vollständige maschinenlesbare Schema liegt unter:

- JSON: `GET /api/openapi` (eingeloggt, JWT-USER)
- Swagger-UI: `GET /api/swagger-ui.html`

Externe Programme können das Schema lokal cachen, um Client-Code zu generieren — z. B. via
[openapi-generator](https://openapi-generator.tech/).
