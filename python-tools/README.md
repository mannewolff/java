# python-tools

FastAPI-Microservice für Bild-Werkzeuge der persönlichen Toolbox.

## Endpoints

| Methode | Pfad | Beschreibung |
|---|---|---|
| `POST` | `/remove-bg` | Hintergrund per [rembg](https://github.com/danielgatis/rembg) (U2Net-Modell) entfernen. Returnt PNG mit Alpha. |
| `POST` | `/crop` | Cover-fit-Crop auf 1200×630 (OpenGraph / WordPress). Returnt JPEG. |
| `POST` | `/palette` | Dominante Farben per [colorthief](https://github.com/fengsp/color-thief-py). Returnt JSON. |
| `GET`  | `/health` | Liveness-Check für Docker- und Spring-Healthchecks. |

### `POST /remove-bg`

| Feld | Typ | Default | Beschreibung |
|---|---|---|---|
| `file` | multipart-Datei | — | PNG / JPEG / WEBP, max 10 MiB |

Response: `image/png` mit Alpha-Kanal.

### `POST /crop`

| Feld | Typ | Default | Range | Beschreibung |
|---|---|---|---|---|
| `file` | multipart-Datei | — | — | PNG / JPEG / WEBP, max 10 MiB |
| `y_offset` | float | 0.5 | 0.0–1.0 | Vertikale Position des Crops (0 = oben, 1 = unten). Wird ignoriert, wenn die Quelle horizontal überhängt — dann wird horizontal zentriert. |
| `quality` | int | 88 | 50–95 | JPEG-Quality |

Response: `image/jpeg`, exakt 1200×630 Pixel.

### `POST /palette`

| Feld | Typ | Default | Range | Beschreibung |
|---|---|---|---|---|
| `file` | multipart-Datei | — | — | PNG / JPEG / WEBP, max 10 MiB |
| `count` | int | 6 | 2–10 | Anzahl gewünschter Farben |

Response: `application/json`, z. B. `{"colors":["#aabbcc","#001122",...]}` — Reihenfolge nach Dominanz.

## Fehler

| Status | Bedeutung |
|---|---|
| 400 | Datei leer |
| 413 | Datei > 10 MiB |
| 415 | Content-Type nicht unterstützt |
| 422 | Form-Parameter ausserhalb Range |
| 500 | Verarbeitung fehlgeschlagen — Body enthält `Background removal failed: <ExceptionClass>: <Message>` (analog für Crop / Palette) |

Bei 500-Fehlern steht der volle Traceback in den Container-Logs (`docker compose logs python-tools`).

## Lokal entwickeln

```bash
python3 -m venv .venv
source .venv/bin/activate
pip install -r requirements-dev.txt
uvicorn main:app --reload
```

Erster `POST /remove-bg`-Aufruf lädt das U2Net-Modell (~180 MB) nach `~/.u2net/`. Im Docker-Build ist es bereits vorgeladen.

## Tests

Tests laufen mit gemockten Schwergewichten — `rembg` und `colorthief` werden über lazy imports + monkeypatch ersetzt. Für die `/crop`-Tests wird ein echtes Pillow erwartet, das aber Teil von `requirements.txt` ist.

```bash
.venv/bin/python -m pytest --cov=. --cov-report=term
```

Aktueller Stand: 29 Tests, 100 % Coverage auf `main.py`.

## Docker

Eigenes Dockerfile in diesem Verzeichnis (`python:3.12-slim`). U2Net wird beim Build vorgeladen, der Container startet mit `uvicorn main:app --host 0.0.0.0 --port 8000` und meldet `healthy`, sobald `/health` antwortet. `docker-compose.yml` im Projekt-Root bindet ihn als Service `python-tools` ein; das Spring-Backend erreicht ihn unter `http://python-tools:8000`.

Cache-Verzeichnisse (numba JIT, matplotlib) liegen in `/tmp` und gehören dem `app`-Nutzer, damit Non-Root-Runtime sauber funktioniert.

## Lizenzen der Abhängigkeiten

- `rembg` — MIT
- `pillow` — MIT-CMU (PIL-Lizenz)
- `colorthief` — BSD-2-Clause
- `fastapi`, `uvicorn`, `python-multipart` — MIT
