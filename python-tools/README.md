# python-tools

FastAPI-Microservice für Bild-Werkzeuge der persönlichen Toolbox. Aktuell:

| Endpoint | Beschreibung |
|---|---|
| `POST /remove-bg` | Multipart-Upload `file`, entfernt den Hintergrund per [rembg](https://github.com/danielgatis/rembg) (U2Net-Modell), liefert PNG mit Alpha-Kanal |
| `GET /health` | Liveness-Check für Docker- und Spring-Healthchecks |

Validierungen:
- Content-Type muss `image/png`, `image/jpeg` oder `image/webp` sein → sonst HTTP 415
- Datei muss nicht-leer sein → sonst HTTP 400
- Max. 10 MiB → sonst HTTP 413

## Lokal entwickeln

```bash
python3 -m venv .venv
source .venv/bin/activate
pip install -r requirements-dev.txt
uvicorn main:app --reload
```

Erster `POST /remove-bg`-Aufruf lädt das U2Net-Modell (~180 MB) nach `~/.u2net/`. Im Docker-Build ist es bereits vorgeladen.

## Tests

Tests laufen mit gemocktem rembg — keine Installation der schweren ML-Abhängigkeiten nötig (ohne `rembg`/`pillow` reichen `fastapi`, `httpx`, `pytest`, `pytest-cov`, `python-multipart`).

```bash
.venv/bin/python -m pytest --cov=. --cov-report=term
```

Aktueller Stand: 10 Tests, 100 % Coverage.

## Docker

Eigenes Dockerfile in diesem Verzeichnis (`python:3.12-slim`). U2Net wird beim Build vorgeladen, der Container startet mit `uvicorn main:app --host 0.0.0.0 --port 8000` und meldet `healthy`, sobald `/health` antwortet. `docker-compose.yml` im Projekt-Root bindet ihn als Service `python-tools` ein; das Spring-Backend erreicht ihn unter `http://python-tools:8000`.
