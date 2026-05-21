"""FastAPI-Microservice fuer Bild-Werkzeuge der persoenlichen Toolbox.

Endpoints:

- POST /remove-bg -> rembg-basierter Hintergrund-Removal, liefert PNG mit Alpha.
- POST /crop      -> Pillow-basierter Cover-Crop auf 1200x630 (OpenGraph / WordPress).
- GET  /health    -> Liveness-Check fuer Docker-Healthcheck und Spring.
"""

from __future__ import annotations

import io
import logging
import traceback
from typing import Annotated

from fastapi import FastAPI, File, Form, HTTPException, UploadFile
from fastapi.responses import Response

logger = logging.getLogger("python-tools")

ALLOWED_CONTENT_TYPES: frozenset[str] = frozenset(
    {"image/png", "image/jpeg", "image/jpg", "image/webp"}
)
MAX_BYTES: int = 10 * 1024 * 1024  # 10 MiB

OG_TARGET_WIDTH: int = 1200
OG_TARGET_HEIGHT: int = 630

app = FastAPI(title="python-tools", version="0.1.0")


def _remove_background(data: bytes) -> bytes:
    """Wrap rembg.remove lazily so tests can run ohne rembg-Installation."""
    from rembg import remove  # local import: keeps rembg out of test imports

    return remove(data)


def _crop_to_og(data: bytes, y_offset: float, quality: int) -> bytes:
    """Cover-fit crop auf 1200x630, vertikal entlang y_offset (0=oben, 1=unten)."""
    from PIL import Image  # local import: tests fuer andere Endpoints brauchen kein Pillow

    src = Image.open(io.BytesIO(data))
    src = src.convert("RGB")
    src_w, src_h = src.size

    target_ratio = OG_TARGET_WIDTH / OG_TARGET_HEIGHT
    src_ratio = src_w / src_h

    if src_ratio > target_ratio:
        # Quelle ist breiter -> Hoehe fuellen, links/rechts beschneiden (horizontal zentriert).
        new_h = OG_TARGET_HEIGHT
        new_w = max(OG_TARGET_WIDTH, round(src_w * (OG_TARGET_HEIGHT / src_h)))
        resized = src.resize((new_w, new_h), Image.LANCZOS)
        x_off = round((new_w - OG_TARGET_WIDTH) * 0.5)
        box = (x_off, 0, x_off + OG_TARGET_WIDTH, OG_TARGET_HEIGHT)
    else:
        # Quelle ist hoeher (oder gleichgroß) -> Breite fuellen, oben/unten via y_offset beschneiden.
        new_w = OG_TARGET_WIDTH
        new_h = max(OG_TARGET_HEIGHT, round(src_h * (OG_TARGET_WIDTH / src_w)))
        resized = src.resize((new_w, new_h), Image.LANCZOS)
        y_off = round((new_h - OG_TARGET_HEIGHT) * y_offset)
        box = (0, y_off, OG_TARGET_WIDTH, y_off + OG_TARGET_HEIGHT)

    cropped = resized.crop(box)
    buf = io.BytesIO()
    cropped.save(buf, format="JPEG", quality=quality, optimize=True)
    return buf.getvalue()


async def _read_and_validate(file: UploadFile) -> bytes:
    """Standard-Upload-Validierung: erlaubter Content-Type, nicht leer, nicht zu gross."""
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
    return contents


@app.get("/health")
def health() -> dict[str, str]:
    """Liveness-Check fuer Docker-Healthcheck und Spring-Discovery."""
    return {"status": "ok"}


@app.post("/remove-bg")
async def remove_bg(file: Annotated[UploadFile, File()]) -> Response:
    """Nimmt ein hochgeladenes Bild, entfernt den Hintergrund, gibt PNG zurueck."""
    contents = await _read_and_validate(file)

    try:
        result = _remove_background(contents)
    except Exception as exc:  # noqa: BLE001 — Wrap any rembg failure
        logger.error("rembg failed", exc_info=True)
        traceback.print_exc()
        detail = f"Background removal failed: {type(exc).__name__}: {exc}"
        raise HTTPException(status_code=500, detail=detail) from exc

    return Response(content=result, media_type="image/png")


@app.post("/crop")
async def crop(
    file: Annotated[UploadFile, File()],
    y_offset: Annotated[float, Form(ge=0.0, le=1.0)] = 0.5,
    quality: Annotated[int, Form(ge=50, le=95)] = 88,
) -> Response:
    """Cover-fit-Crop auf 1200x630 als JPEG; y_offset waehlt vertikalen Anschnitt."""
    contents = await _read_and_validate(file)

    try:
        result = _crop_to_og(contents, y_offset=y_offset, quality=quality)
    except Exception as exc:  # noqa: BLE001 — Wrap any Pillow failure
        logger.error("crop failed", exc_info=True)
        traceback.print_exc()
        detail = f"Crop failed: {type(exc).__name__}: {exc}"
        raise HTTPException(status_code=500, detail=detail) from exc

    return Response(content=result, media_type="image/jpeg")
