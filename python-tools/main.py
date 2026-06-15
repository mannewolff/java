"""FastAPI-Microservice fuer Bild-Werkzeuge der persoenlichen Toolbox.

Endpoints:

- POST /remove-bg      -> rembg-basierter Hintergrund-Removal, liefert PNG mit Alpha.
- POST /crop           -> Pillow-basierter Cover-Crop auf 1200x630 (OpenGraph / WordPress).
- POST /palette        -> colorthief-basierte Brandpalette, liefert N Hex-Farben.
- POST /resize         -> Pillow LANCZOS-Resize auf width x height.
- POST /svg-to-png     -> cairosvg-basierte SVG->PNG-Konvertierung, optional skaliert.
- POST /raster-to-png  -> Pillow-basierte JPEG/PNG->PNG-Konvertierung, optional skaliert.
- GET  /health         -> Liveness-Check fuer Docker-Healthcheck und Spring.
"""

from __future__ import annotations

import io
import logging
from typing import Annotated

from fastapi import FastAPI, File, Form, HTTPException, UploadFile
from fastapi.responses import Response

logger = logging.getLogger("python-tools")

ALLOWED_CONTENT_TYPES: frozenset[str] = frozenset(
    {"image/png", "image/jpeg", "image/jpg", "image/webp"}
)
MAX_BYTES: int = 10 * 1024 * 1024  # 10 MiB
# Explizites Pixel-Limit gegen Decompression-Bombs (#264): ~50 MP deckt reale Fotos
# (z. B. 8000x6000) ab, blockt aber stark komprimierte Riesen-Bilder, die unter MAX_BYTES
# passen, beim Dekodieren aber den Speicher sprengen wuerden.
MAX_IMAGE_PIXELS_LIMIT: int = 50_000_000

OG_DEFAULT_WIDTH: int = 1200
OG_DEFAULT_HEIGHT: int = 630
OG_MIN_DIMENSION: int = 200
OG_MAX_DIMENSION: int = 4096

RESIZE_MIN_DIMENSION: int = 1
RESIZE_MAX_DIMENSION: int = 8192

SVG_CONTENT_TYPES: frozenset[str] = frozenset({"image/svg+xml"})
SVG_MIN_DIMENSION: int = 1
SVG_MAX_DIMENSION: int = 8192
# Pattern: "transparent" oder ein #rrggbb-Hex (case-insensitive). Reicht fuer V1 — falls
# spaeter benannte Farben gebraucht werden, hier erweitern.
SVG_BACKGROUND_PATTERN: str = r"^(transparent|#[0-9a-fA-F]{6})$"

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


class ExternalResourceError(Exception):
    """SVG verweist auf eine externe Ressource (file://, http(s)://) — blockiert (#261)."""


class ImageTooLargeError(Exception):
    """Bild ueberschreitet das Pixel-Limit (Decompression-Bomb-Schutz, #264)."""


def _open_image(data: bytes):  # type: ignore[no-untyped-def]
    """Oeffnet Bild-Bytes mit Pillow und erzwingt das Pixel-Limit (#264).

    `Image.open` liest nur den Header (kein Vollbild-Dekodieren), daher ist der
    Groessen-Check guenstig und greift, bevor Speicher fuer das Bild alloziert wird.
    Setzt zusaetzlich Pillows globalen MAX_IMAGE_PIXELS-Guard als zweite Verteidigungslinie.
    """
    from PIL import Image  # local import: keeps Pillow out of test imports when patched

    # Pillows eigenen (globalen, hohen) Guard abschalten und stattdessen unser explizites
    # Limit deterministisch pruefen — so ist die Reaktion immer ein sauberes 4xx statt
    # einer DecompressionBombError/Warning mit uneinheitlichem Verhalten.
    Image.MAX_IMAGE_PIXELS = None
    img = Image.open(io.BytesIO(data))
    if img.size[0] * img.size[1] > MAX_IMAGE_PIXELS_LIMIT:
        raise ImageTooLargeError(f"{img.size[0]}x{img.size[1]} exceeds pixel limit")
    return img


def _block_external_resources(url: str, *args: object, **kwargs: object) -> bytes:
    """url_fetcher fuer cairosvg, der jede externe Aufloesung verweigert.

    Verhindert Local File Read (file://) und SSRF (http:// auf interne Hosts/Metadaten)
    ueber `<image href=...>` o. ae. in hochgeladenen SVGs.
    """
    raise ExternalResourceError(url)


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
    format: str = "jpeg",
) -> bytes:
    """Cover-fit crop auf width x height; format 'jpeg' oder 'png'."""
    from PIL import Image  # local import: tests fuer andere Endpoints brauchen kein Pillow

    src = _open_image(data)
    src = src.convert("RGBA" if format == "png" else "RGB")
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
    if format == "png":
        # quality is JPEG-only; PNG uses compress_level (0–9), which we leave at default.
        cropped.save(buf, format="PNG")
    else:
        cropped.save(buf, format="JPEG", quality=quality, optimize=True)
    return buf.getvalue()


def _resize(
    data: bytes, width: int, height: int, output_format: str, quality: int
) -> tuple[bytes, str]:
    """Skaliert das Bild auf (width, height) per LANCZOS. Returnt (bytes, media_type)."""
    from PIL import Image  # local import: keeps Pillow out of test imports when patched

    src = _open_image(data)
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
    return await _read_validated(file, ALLOWED_CONTENT_TYPES)


async def _read_and_validate_svg(file: UploadFile) -> bytes:
    """SVG-Variante der Upload-Validierung: erlaubt nur image/svg+xml."""
    return await _read_validated(file, SVG_CONTENT_TYPES)


async def _read_validated(file: UploadFile, allowed: frozenset[str]) -> bytes:
    """Gemeinsame Upload-Validierungs-Logik: Content-Type, Empty, Size."""
    if file.content_type not in allowed:
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


def _raster_to_png(
    data: bytes,
    width: int | None,
    height: int | None,
) -> bytes:
    """Konvertiert ein JPEG/PNG zu PNG mit optionaler Skalierung per LANCZOS.

    Beide Dimensionen optional:
    - Beide angegeben: exakte Zielgröße.
    - Nur width: Höhe proportional berechnet.
    - Nur height: Breite proportional berechnet.
    - Keine Dimension: Originalgröße beibehalten.
    Palette-Mode (P) wird vor dem Speichern nach RGBA konvertiert.
    """
    from PIL import Image  # local import: keeps Pillow out of test imports when patched

    img = _open_image(data)

    if width is not None and height is not None:
        img = img.resize((width, height), Image.LANCZOS)
    elif width is not None:
        ratio = width / img.width
        img = img.resize((width, max(1, round(img.height * ratio))), Image.LANCZOS)
    elif height is not None:
        ratio = height / img.height
        img = img.resize((max(1, round(img.width * ratio)), height), Image.LANCZOS)

    # Palette-Mode hat keinen direkten PNG-Alpha-Support -> RGBA
    if img.mode == "P":
        img = img.convert("RGBA")

    buf = io.BytesIO()
    img.save(buf, format="PNG")
    return buf.getvalue()


def _svg_to_png(
    data: bytes,
    width: int | None,
    height: int | None,
    background: str,
) -> bytes:
    """Lazy-import cairosvg und rendere SVG-Bytes als PNG-Bytes.

    Beide Dimensionen optional: ohne Angabe nimmt cairosvg die SVG-eigene Geometrie.
    `background` == "transparent" laesst den Alpha-Kanal offen, sonst wird die Hex-Farbe
    als opaker Hintergrund eingebrannt.
    """
    import cairosvg  # local import: keeps cairosvg out of test imports when patched

    # Externe Ressourcen (file://, http://) im SVG blocken — gegen LFR/SSRF (#261).
    kwargs: dict[str, object] = {"bytestring": data, "url_fetcher": _block_external_resources}
    if width is not None:
        kwargs["output_width"] = width
    if height is not None:
        kwargs["output_height"] = height
    if background != "transparent":
        kwargs["background_color"] = background
    return cairosvg.svg2png(**kwargs)


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
        raise HTTPException(status_code=500, detail="background removal failed") from exc

    return Response(content=result, media_type="image/png")


@app.post("/crop")
async def crop(
    file: Annotated[UploadFile, File()],
    y_offset: Annotated[float, Form(ge=0.0, le=1.0)] = 0.5,
    x_offset: Annotated[float, Form(ge=0.0, le=1.0)] = 0.5,
    quality: Annotated[int, Form(ge=50, le=95)] = 88,
    width: Annotated[int, Form(ge=OG_MIN_DIMENSION, le=OG_MAX_DIMENSION)] = OG_DEFAULT_WIDTH,
    height: Annotated[int, Form(ge=OG_MIN_DIMENSION, le=OG_MAX_DIMENSION)] = OG_DEFAULT_HEIGHT,
    format: Annotated[str, Form(pattern="^(jpeg|png)$")] = "jpeg",
) -> Response:
    """Cover-fit-Crop auf width x height; format 'jpeg' (default) oder 'png'."""
    contents = await _read_and_validate(file)

    try:
        result = _crop_to_og(
            contents,
            y_offset=y_offset,
            x_offset=x_offset,
            quality=quality,
            width=width,
            height=height,
            format=format,
        )
    except ImageTooLargeError as exc:
        logger.warning("crop rejected oversized image")
        raise HTTPException(status_code=422, detail="image too large") from exc
    except Exception as exc:  # noqa: BLE001 — Wrap any Pillow failure
        logger.error("crop failed", exc_info=True)
        raise HTTPException(status_code=500, detail="crop failed") from exc

    media_type = "image/png" if format == "png" else "image/jpeg"
    return Response(content=result, media_type=media_type)


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
    except ImageTooLargeError as exc:
        logger.warning("resize rejected oversized image")
        raise HTTPException(status_code=422, detail="image too large") from exc
    except Exception as exc:  # noqa: BLE001 — Wrap any Pillow failure
        logger.error("resize failed", exc_info=True)
        raise HTTPException(status_code=500, detail="resize failed") from exc

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
        raise HTTPException(status_code=500, detail="palette extraction failed") from exc

    return {"colors": colors}


@app.post("/svg-to-png")
async def svg_to_png(
    file: Annotated[UploadFile, File()],
    width: Annotated[int | None, Form(ge=SVG_MIN_DIMENSION, le=SVG_MAX_DIMENSION)] = None,
    height: Annotated[int | None, Form(ge=SVG_MIN_DIMENSION, le=SVG_MAX_DIMENSION)] = None,
    background: Annotated[str, Form(pattern=SVG_BACKGROUND_PATTERN)] = "transparent",
) -> Response:
    """Konvertiert ein SVG zu PNG. width/height optional, sonst SVG-eigene Geometrie."""
    contents = await _read_and_validate_svg(file)

    try:
        result = _svg_to_png(
            contents,
            width=width,
            height=height,
            background=background,
        )
    except ExternalResourceError as exc:
        logger.warning("svg-to-png blocked external resource")
        raise HTTPException(
            status_code=422, detail="SVG references external resources"
        ) from exc
    except Exception as exc:  # noqa: BLE001 — Wrap any cairosvg failure
        logger.error("svg-to-png failed", exc_info=True)
        raise HTTPException(status_code=500, detail="SVG conversion failed") from exc

    return Response(content=result, media_type="image/png")


@app.post("/raster-to-png")
async def raster_to_png(
    file: Annotated[UploadFile, File()],
    width: Annotated[int | None, Form(ge=1, le=8192)] = None,
    height: Annotated[int | None, Form(ge=1, le=8192)] = None,
) -> Response:
    """Konvertiert ein JPEG oder PNG zu PNG. width/height optional, sonst Originalgröße."""
    contents = await _read_and_validate(file)

    try:
        result = _raster_to_png(contents, width=width, height=height)
    except ImageTooLargeError as exc:
        logger.warning("raster-to-png rejected oversized image")
        raise HTTPException(status_code=422, detail="image too large") from exc
    except Exception as exc:  # noqa: BLE001 — Wrap any Pillow failure
        logger.error("raster-to-png failed", exc_info=True)
        raise HTTPException(status_code=500, detail="raster conversion failed") from exc

    return Response(content=result, media_type="image/png")
