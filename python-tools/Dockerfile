# syntax=docker/dockerfile:1.7
#
# Multi-Stage-Build: erst eine Builder-Stage mit uv, die `uv sync --frozen`
# laeuft und das venv unter /app/.venv anlegt. Dann eine schlanke Runtime-Stage,
# die nur das venv und main.py uebernimmt. uv wird zur Runtime nicht mehr
# gebraucht.

FROM ghcr.io/astral-sh/uv:python3.12-bookworm-slim AS builder

ENV UV_LINK_MODE=copy \
    UV_COMPILE_BYTECODE=1 \
    UV_PYTHON_DOWNLOADS=never

WORKDIR /app

# Lockfile zuerst kopieren  Layer-Caching: solange pyproject + lockfile sich
# nicht aendern, bleibt die deps-Layer im Cache.
COPY pyproject.toml uv.lock ./
RUN --mount=type=cache,target=/root/.cache/uv \
    uv sync --frozen --no-install-project --no-dev

# Projekt-Code dazu und nochmal sync, damit ggf. das Projekt selbst installiert
# wird (aktuell macht uv hier nur einen Lookup, weil python-tools nicht als
# editable package gebaut wird; das ist aber zukunftssicher).
COPY main.py ./
RUN --mount=type=cache,target=/root/.cache/uv \
    uv sync --frozen --no-dev


FROM python:3.12-slim AS runtime

ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1 \
    U2NET_HOME=/opt/u2net \
    NUMBA_CACHE_DIR=/tmp/numba_cache \
    MPLCONFIGDIR=/tmp/matplotlib \
    PATH="/app/.venv/bin:$PATH"

# OS-deps fuer Pillow + cairosvg-Native + weasyprint (#27) + curl fuer Healthcheck.
# cairosvg/weasyprint laden zur Laufzeit libcairo (-> libcairo2) und benoetigen fuer
# pango-Text-Rendering libpango/libgdk-pixbuf plus shared-mime-info als
# MIME-Lookup-Datenbank. Ohne die Pakete crasht der erste svg2png-Call mit
# OSError "no such file or directory: cairo".
# fonts-dejavu-core: weasyprint braucht mindestens eine Schrift, sonst bleibt der
# PDF-Text leer/als Kaestchen (das Slim-Image bringt keine Fonts mit).
RUN apt-get update \
    && apt-get install -y --no-install-recommends \
        libjpeg62-turbo \
        zlib1g \
        libgomp1 \
        libcairo2 \
        libpangocairo-1.0-0 \
        libgdk-pixbuf-2.0-0 \
        shared-mime-info \
        fonts-dejavu-core \
        curl \
    && rm -rf /var/lib/apt/lists/*

# Non-Root-User mit echtem Home fuer schreibbare Caches.
RUN groupadd --system app \
    && useradd --system --gid app --home-dir /home/app --create-home app

WORKDIR /app

# Venv aus dem Builder uebernehmen.
COPY --from=builder --chown=app:app /app/.venv /app/.venv

# Cache- und Modell-Verzeichnisse vorbereiten.
RUN mkdir -p /opt/u2net /tmp/numba_cache /tmp/matplotlib \
    && chown -R app:app /opt/u2net /tmp/numba_cache /tmp/matplotlib /app

USER app

# U2Net-Modell vorladen, damit der erste Request schnell ist.
RUN python -c "from rembg import new_session; new_session('u2net')"

COPY --chown=app:app main.py ./

EXPOSE 8000

HEALTHCHECK --interval=10s --timeout=3s --start-period=10s --retries=5 \
    CMD curl -fsS http://localhost:8000/health || exit 1

CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8000"]
