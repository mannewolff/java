"""FastAPI-Microservice fuer Bild-Werkzeuge der persoenlichen Toolbox.

Endpoints:

- POST /remove-bg -> rembg-basierter Hintergrund-Removal, liefert PNG mit Alpha.
- POST /crop      -> Pillow-basierter Cover-Crop auf 1200x630 (OpenGraph / WordPress).
- POST /palette   -> colorthief-basierte Brandpalette, liefert N Hex-Farben.
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

OG_DEFAULT_WIDTH: int = 1200
OG_DEFAULT_HEIGHT: int = 630
OG_MIN_DIMENSION: int = 200
OG_MAX_DIMENSION: int = 4096

app = FastAPI(title="python-tools", version="0.1.0")


def _remove_background(data: bytes) -> bytes:
    """Wrap rembg.remove lazily so tests can run ohne rembg-Installation."""
    from rembg import remove  # local import: keeps rembg out of test imports

    return remove(data)


def _extract_palette(data: bytes, count: int) -> list[str]:
    """Lazy-import colorthief und liefere `count` dominante Farben als #rrggbb."""
    from colorthief import ColorThief  # local import: tests koennen ohne colorthief laufen

    thief = ColorThief(io.BytesIO(data))
    if count == 1:
        return [_rgb_to_hex(thief.get_color(quality=10))]
    palette = thief.get_palette(color_count=count, quality=10)
    return [_rgb_to_hex(rgb) for rgb in palette[:count]]


def _rgb_to_hex(rgb: tuple[int, int, int]) -> str:
    return "#{0:02x}{1:02x}{2:02x}".format(rgb[0], rgb[1], rgb[2])


def _crop_to_og(
    data: bytes,
    y_offset: float,
    x_offset: float,
    quality: int,
    width: int,
    height: int,
) -> bytes:
    """Cover-fit crop auf width x height, Ausschnittposition ueber x_offset / y_offset."""
    from PIL import Image  # local import: tests fuer andere Endpoints brauchen kein Pillow

    src = Image.open(io.BytesIO(data))
    src = src.convert("RGB")
    src_w, src_h = src.size

    target_ratio = width / height
    src_ratio = src_w / src_h

    if src_ratio > target_ratio:
        # Quelle ist breiter -> Hoehe fuellen, links/rechts via x_offset beschneiden.
        new_h = height
        new_w = max(width, round(src_w * (height / src_h)))
        resized = src.resize((new_w, new_h), Image.LANCZOS)
        x_off = round((new_w - width) * x_offset)
        box = (x_off, 0, x_off + width, height)
    else:
        # Quelle ist hoeher (oder gleichgroß) -> Breite fuellen, oben/unten via y_offset beschneiden.
        new_w = width
        new_h = max(height, round(src_h * (width / src_w)))
        resized = src.resize((new_w, new_h), Image.LANCZOS)
        y_off = round((new_h - height) * y_offset)
        box = (0, y_off, width, y_off + height)

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
    x_offset: Annotated[float, Form(ge=0.0, le=1.0)] = 0.5,
    quality: Annotated[int, Form(ge=50, le=95)] = 88,
    width: Annotated[int, Form(ge=OG_MIN_DIMENSION, le=OG_MAX_DIMENSION)] = OG_DEFAULT_WIDTH,
    height: Annotated[int, Form(ge=OG_MIN_DIMENSION, le=OG_MAX_DIMENSION)] = OG_DEFAULT_HEIGHT,
) -> Response:
    """Cover-fit-Crop auf width x height als JPEG; x_offset/y_offset waehlen den Anschnitt."""
    contents = await _read_and_validate(file)

    try:
        result = _crop_to_og(
            contents,
            y_offset=y_offset,
            x_offset=x_offset,
            quality=quality,
            width=width,
            height=height,
        )
    except Exception as exc:  # noqa: BLE001 — Wrap any Pillow failure
        logger.error("crop failed", exc_info=True)
        traceback.print_exc()
        detail = f"Crop failed: {type(exc).__name__}: {exc}"
        raise HTTPException(status_code=500, detail=detail) from exc

    return Response(content=result, media_type="image/jpeg")


@app.post("/palette")
async def palette(
    file: Annotated[UploadFile, File()],
    count: Annotated[int, Form(ge=2, le=10)] = 6,
) -> dict[str, list[str]]:
    """Liefert die `count` dominanten Farben als Hex-Strings (gross-nach-klein)."""
    contents = await _read_and_validate(file)

    try:
        colors = _extract_palette(contents, count=count)
    except Exception as exc:  # noqa: BLE001 — Wrap any colorthief failure
        logger.error("palette failed", exc_info=True)
        traceback.print_exc()
        detail = f"Palette extraction failed: {type(exc).__name__}: {exc}"
        raise HTTPException(status_code=500, detail=detail) from exc

    return {"colors": colors}
