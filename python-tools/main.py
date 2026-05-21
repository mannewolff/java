"""FastAPI-Microservice fuer Bild-Werkzeuge der persoenlichen Toolbox.

Aktuell: POST /remove-bg entfernt den Hintergrund eines Bildes mittels rembg
(U2Net-Modell) und liefert ein PNG mit Alpha-Kanal zurueck.
"""

from __future__ import annotations

import logging
import traceback
from typing import Annotated

from fastapi import FastAPI, File, HTTPException, UploadFile
from fastapi.responses import Response

logger = logging.getLogger("python-tools")

ALLOWED_CONTENT_TYPES: frozenset[str] = frozenset(
    {"image/png", "image/jpeg", "image/jpg", "image/webp"}
)
MAX_BYTES: int = 10 * 1024 * 1024  # 10 MiB

app = FastAPI(title="python-tools", version="0.1.0")


def _remove_background(data: bytes) -> bytes:
    """Wrap rembg.remove lazily so tests can run ohne rembg-Installation."""
    from rembg import remove  # local import: keeps rembg out of test imports

    return remove(data)


@app.get("/health")
def health() -> dict[str, str]:
    """Liveness-Check fuer Docker-Healthcheck und Spring-Discovery."""
    return {"status": "ok"}


@app.post("/remove-bg")
async def remove_bg(file: Annotated[UploadFile, File()]) -> Response:
    """Nimmt ein hochgeladenes Bild, entfernt den Hintergrund, gibt PNG zurueck."""
    if file.content_type not in ALLOWED_CONTENT_TYPES:
        raise HTTPException(
            status_code=415,
            detail=f"Unsupported content type: {file.content_type}",
        )

    contents = await file.read()
    if not contents:
        raise HTTPException(status_code=400, detail="Empty file")
    if len(contents) > MAX_BYTES:
        raise HTTPException(status_code=413, detail="File too large")

    try:
        result = _remove_background(contents)
    except Exception as exc:  # noqa: BLE001 — Wrap any rembg failure
        # Log full traceback so the operator can diagnose via
        # `docker compose logs python-tools`, and echo class + message in
        # the HTTP response so the UI alert is actually useful.
        logger.error("rembg failed", exc_info=True)
        traceback.print_exc()
        detail = f"Background removal failed: {type(exc).__name__}: {exc}"
        raise HTTPException(status_code=500, detail=detail) from exc

    return Response(content=result, media_type="image/png")
