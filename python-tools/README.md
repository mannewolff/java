# python-tools

FastAPI-Microservice für Bild-Werkzeuge der persönlichen Toolbox.

## Endpoints

| Methode | Pfad | Beschreibung |
|---|---|---|
| `POST` | `/remove-bg` | Hintergrund per [rembg](https://github.com/danielgatis/rembg) (U2Net-Modell) entfernen. Returnt PNG mit Alpha. |
| `POST` | `/crop` | Cover-fit-Crop auf 1200×630 (OpenGraph / WordPress). Returnt JPEG. |
| `POST` | `/palette` | Dominante Farben per [colorthief](https://github.com/fengsp/color-thief-py). Returnt JSON. |
| `POST` | `/resize` | Proportionales (oder freies) Skalieren via Pillow LANCZOS. Returnt Bild im gewählten Format. |
| `POST` | `/svg-to-png` | SVG zu PNG via [cairosvg](https://cairosvg.org/). Optional `width`/`height`/`background`. |
| `POST` | `/md-to-pdf` | Markdown zu PDF via [weasyprint](https://weasyprint.org/). Form-Feld `markdown`. Externe Ressourcen werden geblockt (#27/#261). |
| `GET`  | `/health` | Liveness-Check für Docker- und Spring-Healthchecks. |

> Alle `POST`-Tool-Endpoints erfordern den `X-Internal-Key`-Header (Internal-Auth, #265); `/health` ist frei. Der Markdown→PDF-Endpoint braucht zur Laufzeit cairo/pango + eine Schrift (`fonts-dejavu-core`, im Dockerfile enthalten).

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

### `POST /resize`

| Feld | Typ | Default | Range | Beschreibung |
|---|---|---|---|---|
| `file` | multipart-Datei | — | — | PNG / JPEG / WEBP, max 10 MiB |
| `width` | int | — | 1–8192 | Zielbreite in Pixeln (Pflicht) |
| `height` | int | — | 1–8192 | Zielhöhe in Pixeln (Pflicht) |
| `output_format` | string | `auto` | `auto`/`png`/`jpeg`/`webp` | `auto` behält das Quellformat (BMP/u.ä. → PNG) |
| `quality` | int | 90 | 50–95 | JPEG-/WEBP-Quality, wird bei `png` ignoriert |

Response: `image/*` passend zum gewählten / erkannten Format, exakt `width × height` Pixel.

### `POST /svg-to-png`

| Feld | Typ | Default | Range | Beschreibung |
|---|---|---|---|---|
| `file` | multipart-Datei | — | — | SVG, Content-Type `image/svg+xml`, max 10 MiB |
| `width` | int | — | 1–8192 | Zielbreite in Pixeln (optional, sonst SVG-eigene Breite) |
| `height` | int | — | 1–8192 | Zielhöhe in Pixeln (optional, sonst SVG-eigene Höhe) |
| `background` | string | `transparent` | `transparent` oder `#rrggbb` | Hintergrundfarbe; `transparent` lässt den Alpha-Kanal offen |

Response: `image/png`. Rendering via [cairosvg](https://cairosvg.org/) (Native-Lib `libcairo`, im Docker-Image vorinstalliert).

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

Setup folgt [CLAUDE-Python.md](../CLAUDE-Python.md) — uv als Paketmanager, `pyproject.toml` + `uv.lock` als Single Source of Truth.

```bash
# uv einmalig installieren (macOS: `brew install uv`, sonst https://docs.astral.sh/uv/)
uv sync --frozen      # installiert prod + dev deps aus uv.lock in .venv/
uv run uvicorn main:app --reload
```

Erster `POST /remove-bg`-Aufruf lädt das U2Net-Modell (~180 MB) nach `~/.u2net/`. Im Docker-Build ist es bereits vorgeladen.

### Lint, Typecheck, Tests

```bash
uv run ruff check .
uv run ruff format --check .
uv run mypy --strict .
uv run pytest         # mit Coverage-Gate (fail_under=90 in pyproject.toml)
```

Pre-Commit-Hooks (`.pre-commit-config.yaml`): einmalig `pre-commit install`, dann laufen `ruff`, `ruff-format`, `mypy`, `trailing-whitespace` und `end-of-file-fixer` automatisch vor jedem Commit.

Tests laufen mit gemockten Schwergewichten — `rembg` und `colorthief` werden über lazy imports + monkeypatch ersetzt. Für die `/crop`-Tests wird ein echtes Pillow erwartet, das aber Teil der prod-deps ist.

Aktueller Stand: 51 Tests, 100 % Coverage auf `main.py`.

## Docker

Eigenes Dockerfile in diesem Verzeichnis (`python:3.12-slim`). U2Net wird beim Build vorgeladen, der Container startet mit `uvicorn main:app --host 0.0.0.0 --port 8000` und meldet `healthy`, sobald `/health` antwortet. `docker-compose.yml` im Projekt-Root bindet ihn als Service `python-tools` ein; das Spring-Backend erreicht ihn unter `http://python-tools:8000`.

Cache-Verzeichnisse (numba JIT, matplotlib) liegen in `/tmp` und gehören dem `app`-Nutzer, damit Non-Root-Runtime sauber funktioniert.

## Lizenzen der Abhängigkeiten

- `rembg` — MIT
- `pillow` — MIT-CMU (PIL-Lizenz)
- `colorthief` — BSD-2-Clause
- `fastapi`, `uvicorn`, `python-multipart` — MIT
