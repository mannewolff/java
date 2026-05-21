# syntax=docker/dockerfile:1.7
FROM python:3.12-slim AS base

ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1 \
    PIP_NO_CACHE_DIR=1 \
    U2NET_HOME=/opt/u2net \
    NUMBA_CACHE_DIR=/tmp/numba_cache \
    MPLCONFIGDIR=/tmp/matplotlib

# OS-deps for Pillow + curl for healthcheck
RUN apt-get update \
    && apt-get install -y --no-install-recommends \
        libjpeg62-turbo \
        zlib1g \
        libgomp1 \
        curl \
    && rm -rf /var/lib/apt/lists/*

# Create non-root user with a real home so caches are writable.
RUN groupadd --system app \
    && useradd --system --gid app --home-dir /home/app --create-home app

WORKDIR /app

# Install Python deps system-wide as root.
COPY requirements.txt ./
RUN pip install --no-cache-dir -r requirements.txt

# Cache + model dirs owned by app so runtime imports + downloads can write.
RUN mkdir -p /opt/u2net /tmp/numba_cache /tmp/matplotlib \
    && chown -R app:app /opt/u2net /tmp/numba_cache /tmp/matplotlib /app

USER app

# Pre-fetch the U2Net model AS the app user so the first request is fast
# and so any numba JIT caches land in app-owned /tmp/numba_cache.
RUN python -c "from rembg import new_session; new_session('u2net')"

COPY --chown=app:app main.py ./

EXPOSE 8000

HEALTHCHECK --interval=10s --timeout=3s --start-period=10s --retries=5 \
    CMD curl -fsS http://localhost:8000/health || exit 1

CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8000"]
