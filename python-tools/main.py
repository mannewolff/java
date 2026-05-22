"""FastAPI-Microservice fuer Bild-Werkzeuge der persoenlichen Toolbox.

Endpoints:

- POST /remove-bg -> rembg-basierter Hintergrund-Removal, liefert PNG mit Alpha.
- POST /crop      -> Pillow-basierter Cover-Crop auf 1200x630 (OpenGraph / WordPress).
- POST /palette   -> colorthief-basierte Brandpalette, liefert N Hex-Farben.
- POST /resize    -> Pillow LANCZOS-Resize auf width x height.
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

RESIZE_MIN_DIMENSION: int = 1
RESIZE_MAX_DIMENSION: int = 8192

FORMAT_TO_MEDIA: dict[str, str] = {
    "PNG": "image/png",
    "JPEG": "image/jpeg",
    "WEBP": "image/webp",
}
MEDIA_TO_PILLOW: dict[str, str] = {
    "png": "PNG",
    "jpeg": "JPEG",
    "webp": "WEBP",
}

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


def _resize(
    data: bytes, width: int, height: int, output_format: str, quality: int
) -> tuple[bytes, str]:
    """Skaliert das Bild auf (width, height) per LANCZOS. Returnt (bytes, media_type)."""
    from PIL import Image  # local import: keeps Pillow out of test imports when patched

    src = Image.open(io.BytesIO(data))
    source_format = (src.format or "PNG").upper()

    if output_format == "auto":
        out_format = source_format if source_format in FORMAT_TO_MEDIA else "PNG"
    else:
        out_format = MEDIA_TO_PILLOW[output_format]

    # JPEG hat keinen Alpha-Kanal -> RGB konvertieren, sonst wirft save TypeError.
    if out_format == "JPEG" and src.mode in ("RGBA", "LA", "P"):
        src = src.convert("RGB")

    resized = src.resize((width, height), Image.LANCZOS)
    buf = io.BytesIO()
    save_kwargs: dict[str, int | bool] = {}
    if out_format in ("JPEG", "WEBP"):
        save_kwargs["quality"] = quality
        save_kwargs["optimize"] = True
    resized.save(buf, format=out_format, **save_kwargs)
    return buf.getvalue(), FORMAT_TO_MEDIA[out_format]


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


@app.post("/resize")
async def resize(
    file: Annotated[UploadFile, File()],
    width: Annotated[int, Form(ge=RESIZE_MIN_DIMENSION, le=RESIZE_MAX_DIMENSION)],
    height: Annotated[int, Form(ge=RESIZE_MIN_DIMENSION, le=RESIZE_MAX_DIMENSION)],
    output_format: Annotated[str, Form(pattern="^(auto|png|jpeg|webp)$")] = "auto",
    quality: Annotated[int, Form(ge=50, le=95)] = 90,
) -> Response:
    """Skaliert ein Bild auf die gewuenschten Pixelmasse (LANCZOS)."""
    contents = await _read_and_validate(file)

    try:
        result, media_type = _resize(
            contents,
            width=width,
            height=height,
            output_format=output_format,
            quality=quality,
        )
    except Exception as exc:  # noqa: BLE001 — Wrap any Pillow failure
        logger.error("resize failed", exc_info=True)
        traceback.print_exc()
        detail = f"Resize failed: {type(exc).__name__}: {exc}"
        raise HTTPException(status_code=500, detail=detail) from exc

    return Response(content=result, media_type=media_type)


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
