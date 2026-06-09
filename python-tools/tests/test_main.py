"""Tests fuer den FastAPI-Microservice (rembg ist gemockt)."""

from __future__ import annotations

import io
import types
from typing import Iterator
from unittest.mock import patch

import pytest
from fastapi.testclient import TestClient
from PIL import Image

import main


# Minimal valid 1x1 PNG (red pixel) used as upload fixture.
TINY_PNG_BYTES: bytes = bytes.fromhex(
    "89504E470D0A1A0A0000000D49484452000000010000000108060000001F15C489"
    "0000000D49444154789C636060606000000005000158C7E54E0000000049454E44"
    "AE426082"
)


@pytest.fixture
def client() -> Iterator[TestClient]:
    with TestClient(main.app) as test_client:
        yield test_client


def test_health_returns_ok(client: TestClient) -> None:
    # When
    response = client.get("/health")

    # Then
    assert response.status_code == 200
    assert response.json() == {"status": "ok"}


def test_remove_bg_happy_path_returns_png(client: TestClient) -> None:
    # Given
    fake_output = b"\x89PNG\r\n\x1a\nfake-rembg-output"

    # When
    with patch.object(main, "_remove_background", return_value=fake_output) as remove:
        response = client.post(
            "/remove-bg",
            files={"file": ("input.png", io.BytesIO(TINY_PNG_BYTES), "image/png")},
        )

    # Then
    assert response.status_code == 200
    assert response.headers["content-type"] == "image/png"
    assert response.content == fake_output
    remove.assert_called_once_with(TINY_PNG_BYTES)


def test_remove_bg_accepts_jpeg(client: TestClient) -> None:
    # When
    with patch.object(main, "_remove_background", return_value=b"png"):
        response = client.post(
            "/remove-bg",
            files={"file": ("input.jpg", io.BytesIO(b"\xff\xd8\xff\xe0"), "image/jpeg")},
        )

    # Then
    assert response.status_code == 200


def test_remove_bg_accepts_webp(client: TestClient) -> None:
    # When
    with patch.object(main, "_remove_background", return_value=b"png"):
        response = client.post(
            "/remove-bg",
            files={"file": ("input.webp", io.BytesIO(b"RIFF"), "image/webp")},
        )

    # Then
    assert response.status_code == 200


def test_remove_bg_rejects_text_content_type(client: TestClient) -> None:
    # When
    response = client.post(
        "/remove-bg",
        files={"file": ("notes.txt", io.BytesIO(b"not an image"), "text/plain")},
    )

    # Then
    assert response.status_code == 415
    assert "Unsupported content type" in response.json()["detail"]


def test_remove_bg_rejects_empty_file(client: TestClient) -> None:
    # When
    response = client.post(
        "/remove-bg",
        files={"file": ("empty.png", io.BytesIO(b""), "image/png")},
    )

    # Then
    assert response.status_code == 400
    assert response.json()["detail"] == "Empty file"


def test_remove_bg_rejects_too_large_file(
    client: TestClient, monkeypatch: pytest.MonkeyPatch
) -> None:
    # Given — drop the limit to 10 bytes so the test stays fast
    monkeypatch.setattr(main, "MAX_BYTES", 10)

    # When
    response = client.post(
        "/remove-bg",
        files={
            "file": (
                "big.png",
                io.BytesIO(b"x" * 100),
                "image/png",
            )
        },
    )

    # Then
    assert response.status_code == 413
    assert response.json()["detail"] == "File too large"


def test_remove_bg_returns_500_when_rembg_raises(client: TestClient) -> None:
    # Given
    def boom(_data: bytes) -> bytes:
        raise RuntimeError("model crashed")

    # When
    with patch.object(main, "_remove_background", side_effect=boom):
        response = client.post(
            "/remove-bg",
            files={"file": ("x.png", io.BytesIO(TINY_PNG_BYTES), "image/png")},
        )

    # Then
    assert response.status_code == 500
    detail = response.json()["detail"]
    assert detail.startswith("Background removal failed:")
    assert "RuntimeError" in detail
    assert "model crashed" in detail


def test_remove_bg_requires_file_field(client: TestClient) -> None:
    # When — FastAPI returns 422 for a missing required form field
    response = client.post("/remove-bg")

    # Then
    assert response.status_code == 422


# ---------------------------------------------------------------------------
# /crop tests
# ---------------------------------------------------------------------------


def _solid_image_bytes(width: int, height: int, fmt: str = "PNG", color=(200, 50, 50)) -> bytes:
    img = Image.new("RGB", (width, height), color)
    buf = io.BytesIO()
    img.save(buf, format=fmt)
    return buf.getvalue()


def _split_image_bytes(width: int, height: int, top_color, bottom_color, fmt: str = "PNG") -> bytes:
    """Bild mit klar getrennten Farbflaechen oben/unten — fuer y_offset-Tests."""
    img = Image.new("RGB", (width, height), top_color)
    bottom = Image.new("RGB", (width, height // 2), bottom_color)
    img.paste(bottom, (0, height // 2))
    buf = io.BytesIO()
    img.save(buf, format=fmt)
    return buf.getvalue()


def _decode(content: bytes) -> Image.Image:
    return Image.open(io.BytesIO(content))


def test_crop_portrait_image_yields_1200x630_jpeg(client: TestClient) -> None:
    # Given — tall input (100x300)
    upload = _solid_image_bytes(100, 300, fmt="PNG")

    # When
    response = client.post(
        "/crop",
        files={"file": ("portrait.png", io.BytesIO(upload), "image/png")},
    )

    # Then
    assert response.status_code == 200
    assert response.headers["content-type"] == "image/jpeg"
    out = _decode(response.content)
    assert out.size == (1200, 630)
    assert out.format == "JPEG"


def test_crop_landscape_image_yields_1200x630_jpeg(client: TestClient) -> None:
    # Given — wider-than-target input
    upload = _solid_image_bytes(3000, 800, fmt="JPEG")

    # When
    response = client.post(
        "/crop",
        files={"file": ("landscape.jpg", io.BytesIO(upload), "image/jpeg")},
    )

    # Then
    assert response.status_code == 200
    out = _decode(response.content)
    assert out.size == (1200, 630)


def test_crop_accepts_webp(client: TestClient) -> None:
    # Given
    upload = _solid_image_bytes(800, 1200, fmt="WEBP")

    # When
    response = client.post(
        "/crop",
        files={"file": ("photo.webp", io.BytesIO(upload), "image/webp")},
    )

    # Then
    assert response.status_code == 200
    out = _decode(response.content)
    assert out.size == (1200, 630)


def test_crop_y_offset_zero_selects_top_band(client: TestClient) -> None:
    # Given — top half red, bottom half blue, taller-than-target ratio
    upload = _split_image_bytes(1200, 1260, top_color=(255, 0, 0), bottom_color=(0, 0, 255))

    # When
    response = client.post(
        "/crop",
        data={"y_offset": "0.0"},
        files={"file": ("split.png", io.BytesIO(upload), "image/png")},
    )

    # Then
    assert response.status_code == 200
    out = _decode(response.content)
    # Mid-top pixel must be predominantly red.
    r, g, b = out.getpixel((600, 100))
    assert r > 180 and g < 80 and b < 80


def test_crop_y_offset_one_selects_bottom_band(client: TestClient) -> None:
    # Given
    upload = _split_image_bytes(1200, 1260, top_color=(255, 0, 0), bottom_color=(0, 0, 255))

    # When
    response = client.post(
        "/crop",
        data={"y_offset": "1.0"},
        files={"file": ("split.png", io.BytesIO(upload), "image/png")},
    )

    # Then
    assert response.status_code == 200
    out = _decode(response.content)
    r, g, b = out.getpixel((600, 500))
    assert b > 180 and r < 80 and g < 80


def _side_split_image_bytes(width: int, height: int, left_color, right_color) -> bytes:
    """Bild mit klar getrennten Farbflaechen links/rechts — fuer x_offset-Tests."""
    img = Image.new("RGB", (width, height), left_color)
    right = Image.new("RGB", (width // 2, height), right_color)
    img.paste(right, (width // 2, 0))
    buf = io.BytesIO()
    img.save(buf, format="PNG")
    return buf.getvalue()


def test_crop_x_offset_zero_selects_left_band(client: TestClient) -> None:
    # Given — landscape (wider than 1200x630), left red, right blue
    upload = _side_split_image_bytes(2400, 630, left_color=(255, 0, 0), right_color=(0, 0, 255))

    # When
    response = client.post(
        "/crop",
        data={"x_offset": "0.0"},
        files={"file": ("split.png", io.BytesIO(upload), "image/png")},
    )

    # Then
    assert response.status_code == 200
    out = _decode(response.content)
    r, g, b = out.getpixel((100, 300))
    assert r > 180 and g < 80 and b < 80


def test_crop_x_offset_one_selects_right_band(client: TestClient) -> None:
    upload = _side_split_image_bytes(2400, 630, left_color=(255, 0, 0), right_color=(0, 0, 255))

    response = client.post(
        "/crop",
        data={"x_offset": "1.0"},
        files={"file": ("split.png", io.BytesIO(upload), "image/png")},
    )

    assert response.status_code == 200
    out = _decode(response.content)
    r, g, b = out.getpixel((1100, 300))
    assert b > 180 and r < 80 and g < 80


def test_crop_rejects_x_offset_out_of_range(client: TestClient) -> None:
    upload = _solid_image_bytes(800, 1200)
    response = client.post(
        "/crop",
        data={"x_offset": "1.5"},
        files={"file": ("x.png", io.BytesIO(upload), "image/png")},
    )
    assert response.status_code == 422


def test_crop_rejects_text_content_type(client: TestClient) -> None:
    response = client.post(
        "/crop",
        files={"file": ("notes.txt", io.BytesIO(b"not an image"), "text/plain")},
    )

    assert response.status_code == 415


def test_crop_rejects_empty_file(client: TestClient) -> None:
    response = client.post(
        "/crop",
        files={"file": ("empty.png", io.BytesIO(b""), "image/png")},
    )

    assert response.status_code == 400


def test_crop_rejects_too_large_file(client: TestClient, monkeypatch: pytest.MonkeyPatch) -> None:
    monkeypatch.setattr(main, "MAX_BYTES", 10)

    response = client.post(
        "/crop",
        files={"file": ("big.png", io.BytesIO(b"x" * 100), "image/png")},
    )

    assert response.status_code == 413


def test_crop_rejects_y_offset_out_of_range(client: TestClient) -> None:
    upload = _solid_image_bytes(800, 1200)
    response = client.post(
        "/crop",
        data={"y_offset": "1.5"},
        files={"file": ("x.png", io.BytesIO(upload), "image/png")},
    )

    assert response.status_code == 422


def test_crop_with_custom_dimensions_yields_those_dimensions(client: TestClient) -> None:
    # Given
    upload = _solid_image_bytes(2000, 2000, fmt="PNG")

    # When — Twitter Card 1200x675
    response = client.post(
        "/crop",
        data={"width": "1200", "height": "675"},
        files={"file": ("photo.png", io.BytesIO(upload), "image/png")},
    )

    # Then
    assert response.status_code == 200
    out = _decode(response.content)
    assert out.size == (1200, 675)


def test_crop_with_square_dimensions(client: TestClient) -> None:
    upload = _solid_image_bytes(2000, 2000, fmt="PNG")

    response = client.post(
        "/crop",
        data={"width": "1080", "height": "1080"},
        files={"file": ("photo.png", io.BytesIO(upload), "image/png")},
    )

    assert response.status_code == 200
    assert _decode(response.content).size == (1080, 1080)


def test_crop_rejects_width_below_min(client: TestClient) -> None:
    upload = _solid_image_bytes(800, 1200)
    response = client.post(
        "/crop",
        data={"width": "100"},
        files={"file": ("x.png", io.BytesIO(upload), "image/png")},
    )
    assert response.status_code == 422


def test_crop_rejects_width_above_max(client: TestClient) -> None:
    upload = _solid_image_bytes(800, 1200)
    response = client.post(
        "/crop",
        data={"width": "5000"},
        files={"file": ("x.png", io.BytesIO(upload), "image/png")},
    )
    assert response.status_code == 422


def test_crop_rejects_height_out_of_range(client: TestClient) -> None:
    upload = _solid_image_bytes(800, 1200)
    response = client.post(
        "/crop",
        data={"height": "100"},
        files={"file": ("x.png", io.BytesIO(upload), "image/png")},
    )
    assert response.status_code == 422


def test_crop_rejects_quality_out_of_range(client: TestClient) -> None:
    upload = _solid_image_bytes(800, 1200)
    response = client.post(
        "/crop",
        data={"quality": "10"},
        files={"file": ("x.png", io.BytesIO(upload), "image/png")},
    )

    assert response.status_code == 422


def test_crop_returns_png_when_format_is_png(client: TestClient) -> None:
    upload = _solid_image_bytes(800, 1200, fmt="PNG")

    response = client.post(
        "/crop",
        data={"format": "png"},
        files={"file": ("x.png", io.BytesIO(upload), "image/png")},
    )

    assert response.status_code == 200
    assert response.headers["content-type"] == "image/png"
    out = _decode(response.content)
    assert out.format == "PNG"
    assert out.size == (1200, 630)


def test_crop_preserves_alpha_channel_for_png_format(client: TestClient) -> None:
    """RGBA-Quelle + format=png → Output muss Alpha-Kanal behalten (RGB-Konvertierung wäre falsch)."""
    img = Image.new("RGBA", (800, 1200), (100, 150, 200, 128))
    buf = io.BytesIO()
    img.save(buf, format="PNG")
    upload = buf.getvalue()

    response = client.post(
        "/crop",
        data={"format": "png"},
        files={"file": ("x.png", io.BytesIO(upload), "image/png")},
    )

    assert response.status_code == 200
    out = _decode(response.content)
    assert out.mode == "RGBA", f"Expected RGBA, got {out.mode}"


def test_crop_rejects_invalid_format(client: TestClient) -> None:
    upload = _solid_image_bytes(800, 1200)

    response = client.post(
        "/crop",
        data={"format": "gif"},
        files={"file": ("x.png", io.BytesIO(upload), "image/png")},
    )

    assert response.status_code == 422


def test_crop_returns_500_when_pillow_raises(
    client: TestClient, monkeypatch: pytest.MonkeyPatch
) -> None:
    # Given
    def boom(
        _data: bytes,
        *,
        y_offset: float,
        x_offset: float,
        quality: int,
        width: int,
        height: int,
        format: str,
    ) -> bytes:
        raise RuntimeError("pillow crashed")

    monkeypatch.setattr(main, "_crop_to_og", boom)
    upload = _solid_image_bytes(800, 1200)

    # When
    response = client.post(
        "/crop",
        files={"file": ("x.png", io.BytesIO(upload), "image/png")},
    )

    # Then
    assert response.status_code == 500
    detail = response.json()["detail"]
    assert detail.startswith("Crop failed:")
    assert "RuntimeError" in detail
    assert "pillow crashed" in detail


# ---------------------------------------------------------------------------
# /resize tests
# ---------------------------------------------------------------------------


def test_resize_happy_path_returns_target_dimensions(client: TestClient) -> None:
    upload = _solid_image_bytes(800, 600, fmt="PNG")

    response = client.post(
        "/resize",
        data={"width": "400", "height": "300"},
        files={"file": ("photo.png", io.BytesIO(upload), "image/png")},
    )

    assert response.status_code == 200
    assert response.headers["content-type"] == "image/png"
    out = _decode(response.content)
    assert out.size == (400, 300)


def test_resize_auto_format_preserves_jpeg(client: TestClient) -> None:
    upload = _solid_image_bytes(800, 600, fmt="JPEG")

    response = client.post(
        "/resize",
        data={"width": "400", "height": "300"},
        files={"file": ("photo.jpg", io.BytesIO(upload), "image/jpeg")},
    )

    assert response.status_code == 200
    assert response.headers["content-type"] == "image/jpeg"
    out = _decode(response.content)
    assert out.format == "JPEG"


def test_resize_explicit_format_overrides_source(client: TestClient) -> None:
    upload = _solid_image_bytes(800, 600, fmt="PNG")

    response = client.post(
        "/resize",
        data={"width": "400", "height": "300", "output_format": "jpeg"},
        files={"file": ("photo.png", io.BytesIO(upload), "image/png")},
    )

    assert response.status_code == 200
    assert response.headers["content-type"] == "image/jpeg"
    out = _decode(response.content)
    assert out.format == "JPEG"


def test_resize_jpeg_quality_affects_size(client: TestClient) -> None:
    # Use a varied input so JPEG quality has something to compress.
    upload = _split_image_bytes(800, 600, top_color=(180, 30, 30), bottom_color=(20, 60, 200))

    def post(quality: int) -> bytes:
        response = client.post(
            "/resize",
            data={
                "width": "400",
                "height": "300",
                "output_format": "jpeg",
                "quality": str(quality),
            },
            files={"file": ("photo.png", io.BytesIO(upload), "image/png")},
        )
        assert response.status_code == 200
        return response.content

    low = post(50)
    high = post(95)
    assert len(low) < len(high)


def test_resize_png_with_alpha_to_jpeg_strips_alpha(client: TestClient) -> None:
    from PIL import Image

    img = Image.new("RGBA", (400, 300), (255, 0, 0, 128))
    buf = io.BytesIO()
    img.save(buf, format="PNG")

    response = client.post(
        "/resize",
        data={"width": "200", "height": "150", "output_format": "jpeg"},
        files={"file": ("photo.png", io.BytesIO(buf.getvalue()), "image/png")},
    )

    assert response.status_code == 200
    out = _decode(response.content)
    assert out.mode == "RGB"


def test_resize_rejects_width_below_min(client: TestClient) -> None:
    upload = _solid_image_bytes(800, 600)
    response = client.post(
        "/resize",
        data={"width": "0", "height": "300"},
        files={"file": ("photo.png", io.BytesIO(upload), "image/png")},
    )
    assert response.status_code == 422


def test_resize_rejects_height_above_max(client: TestClient) -> None:
    upload = _solid_image_bytes(800, 600)
    response = client.post(
        "/resize",
        data={"width": "400", "height": "9000"},
        files={"file": ("photo.png", io.BytesIO(upload), "image/png")},
    )
    assert response.status_code == 422


def test_resize_rejects_unknown_format(client: TestClient) -> None:
    upload = _solid_image_bytes(800, 600)
    response = client.post(
        "/resize",
        data={"width": "400", "height": "300", "output_format": "bmp"},
        files={"file": ("photo.png", io.BytesIO(upload), "image/png")},
    )
    assert response.status_code == 422


def test_resize_rejects_text_content_type(client: TestClient) -> None:
    response = client.post(
        "/resize",
        data={"width": "400", "height": "300"},
        files={"file": ("notes.txt", io.BytesIO(b"hello"), "text/plain")},
    )
    assert response.status_code == 415


def test_resize_rejects_empty_file(client: TestClient) -> None:
    response = client.post(
        "/resize",
        data={"width": "400", "height": "300"},
        files={"file": ("empty.png", io.BytesIO(b""), "image/png")},
    )
    assert response.status_code == 400


def test_resize_rejects_too_large_file(
    client: TestClient, monkeypatch: pytest.MonkeyPatch
) -> None:
    monkeypatch.setattr(main, "MAX_BYTES", 10)
    upload = _solid_image_bytes(800, 600)
    response = client.post(
        "/resize",
        data={"width": "400", "height": "300"},
        files={"file": ("big.png", io.BytesIO(upload), "image/png")},
    )
    assert response.status_code == 413


def test_resize_returns_500_when_pillow_raises(
    client: TestClient, monkeypatch: pytest.MonkeyPatch
) -> None:
    def boom(
        _data: bytes, *, width: int, height: int, output_format: str, quality: int
    ) -> tuple[bytes, str]:
        raise RuntimeError("pillow crashed")

    monkeypatch.setattr(main, "_resize", boom)
    upload = _solid_image_bytes(800, 600)

    response = client.post(
        "/resize",
        data={"width": "400", "height": "300"},
        files={"file": ("photo.png", io.BytesIO(upload), "image/png")},
    )

    assert response.status_code == 500
    detail = response.json()["detail"]
    assert detail.startswith("Resize failed:")
    assert "RuntimeError" in detail


def test_resize_preserves_whole_image_not_a_crop(client: TestClient) -> None:
    """A 2000x1000 source with red top half + blue bottom half MUST still show
    both colors after resizing to 200x100. A crop would lose one of them."""
    upload = _split_image_bytes(
        2000, 1000, top_color=(220, 30, 30), bottom_color=(20, 60, 220), fmt="PNG"
    )

    response = client.post(
        "/resize",
        data={"width": "200", "height": "100"},
        files={"file": ("photo.png", io.BytesIO(upload), "image/png")},
    )

    assert response.status_code == 200
    out = _decode(response.content).convert("RGB")
    assert out.size == (200, 100)
    # Top quarter must still be predominantly red.
    r_top, g_top, b_top = out.getpixel((100, 20))
    assert r_top > 150 and g_top < 80 and b_top < 80, f"top pixel was {(r_top, g_top, b_top)}"
    # Bottom quarter must still be predominantly blue.
    r_bot, g_bot, b_bot = out.getpixel((100, 80))
    assert b_bot > 150 and r_bot < 80 and g_bot < 80, f"bottom pixel was {(r_bot, g_bot, b_bot)}"


def test_resize_unknown_source_format_defaults_to_png() -> None:
    """When auto-mode meets an unusual source, fall back to PNG output."""
    import io as _io
    from PIL import Image

    # Construct a BMP — Pillow knows it, but it is not in FORMAT_TO_MEDIA.
    img = Image.new("RGB", (50, 50), (10, 20, 30))
    buf = _io.BytesIO()
    img.save(buf, format="BMP")

    out_bytes, media = main._resize(
        buf.getvalue(), width=10, height=10, output_format="auto", quality=90
    )

    assert media == "image/png"
    out = main.Image if False else None  # noqa: F841 — keep type stub silent
    decoded = _decode(out_bytes)
    assert decoded.format == "PNG"


# ---------------------------------------------------------------------------
# /palette tests
# ---------------------------------------------------------------------------


def test_palette_happy_path_returns_hex_colors(
    client: TestClient, monkeypatch: pytest.MonkeyPatch
) -> None:
    # Given — replace colorthief at the wrapper level to keep the test fast.
    monkeypatch.setattr(
        main,
        "_extract_palette",
        lambda data, count: ["#aabbcc", "#001122", "#abcdef"][:count],
    )

    # When
    response = client.post(
        "/palette",
        data={"count": "3"},
        files={"file": ("x.png", io.BytesIO(_solid_image_bytes(100, 100)), "image/png")},
    )

    # Then
    assert response.status_code == 200
    body = response.json()
    assert body == {"colors": ["#aabbcc", "#001122", "#abcdef"]}


def test_palette_default_count_is_six(
    client: TestClient, monkeypatch: pytest.MonkeyPatch
) -> None:
    captured: list[int] = []

    def fake(data: bytes, count: int) -> list[str]:
        captured.append(count)
        return ["#000000"] * count

    monkeypatch.setattr(main, "_extract_palette", fake)

    response = client.post(
        "/palette",
        files={"file": ("x.png", io.BytesIO(_solid_image_bytes(100, 100)), "image/png")},
    )

    assert response.status_code == 200
    assert captured == [6]
    assert len(response.json()["colors"]) == 6


def test_palette_count_out_of_range_low(client: TestClient) -> None:
    response = client.post(
        "/palette",
        data={"count": "1"},
        files={"file": ("x.png", io.BytesIO(_solid_image_bytes(100, 100)), "image/png")},
    )
    assert response.status_code == 422


def test_palette_count_out_of_range_high(client: TestClient) -> None:
    response = client.post(
        "/palette",
        data={"count": "11"},
        files={"file": ("x.png", io.BytesIO(_solid_image_bytes(100, 100)), "image/png")},
    )
    assert response.status_code == 422


def test_palette_rejects_text_content_type(client: TestClient) -> None:
    response = client.post(
        "/palette",
        files={"file": ("notes.txt", io.BytesIO(b"not an image"), "text/plain")},
    )
    assert response.status_code == 415


def test_palette_returns_500_when_colorthief_raises(
    client: TestClient, monkeypatch: pytest.MonkeyPatch
) -> None:
    def boom(data: bytes, count: int) -> list[str]:
        raise RuntimeError("kmeans crashed")

    monkeypatch.setattr(main, "_extract_palette", boom)

    response = client.post(
        "/palette",
        files={"file": ("x.png", io.BytesIO(_solid_image_bytes(100, 100)), "image/png")},
    )

    assert response.status_code == 500
    detail = response.json()["detail"]
    assert detail.startswith("Palette extraction failed:")
    assert "RuntimeError" in detail


def test_rgb_to_hex_formats_lowercase_hex() -> None:
    assert main._rgb_to_hex((255, 0, 0)) == "#ff0000"
    assert main._rgb_to_hex((0, 255, 0)) == "#00ff00"
    assert main._rgb_to_hex((0, 0, 255)) == "#0000ff"
    assert main._rgb_to_hex((171, 205, 239)) == "#abcdef"


def test_extract_palette_invokes_colorthief() -> None:
    # Given — fake colorthief module with the get_palette/get_color API.
    fake_colorthief = types.ModuleType("colorthief")

    class FakeThief:
        def __init__(self, _stream) -> None:
            pass

        def get_palette(self, color_count: int, quality: int) -> list[tuple[int, int, int]]:
            return [(255, 0, 0), (0, 255, 0), (0, 0, 255)][:color_count]

        def get_color(self, quality: int) -> tuple[int, int, int]:
            return (10, 20, 30)

    fake_colorthief.ColorThief = FakeThief  # type: ignore[attr-defined]

    # When
    with patch.dict("sys.modules", {"colorthief": fake_colorthief}):
        palette_n = main._extract_palette(b"raw", count=2)
        palette_one = main._extract_palette(b"raw", count=1)

    # Then
    assert palette_n == ["#ff0000", "#00ff00"]
    assert palette_one == ["#0a141e"]


# ---------------------------------------------------------------------------


def test_remove_background_invokes_rembg() -> None:
    """The lazy-import wrapper must call rembg.remove with the given bytes."""
    # Given
    fake_rembg = types.ModuleType("rembg")
    fake_rembg.remove = lambda data: data + b":processed"  # type: ignore[attr-defined]

    # When
    with patch.dict("sys.modules", {"rembg": fake_rembg}):
        result = main._remove_background(b"input")

    # Then
    assert result == b"input:processed"


# ---------------------------------------------------------------------------
# /svg-to-png
# ---------------------------------------------------------------------------


TINY_SVG_BYTES: bytes = (
    b'<?xml version="1.0" standalone="no"?>'
    b'<svg xmlns="http://www.w3.org/2000/svg" width="10" height="10">'
    b'<rect width="10" height="10" fill="red"/></svg>'
)


def test_svg_to_png_happy_path_returns_png(client: TestClient) -> None:
    # Given
    fake_png = b"\x89PNG\r\n\x1a\nfake-cairosvg-output"

    # When
    with patch.object(main, "_svg_to_png", return_value=fake_png) as conv:
        response = client.post(
            "/svg-to-png",
            files={"file": ("input.svg", io.BytesIO(TINY_SVG_BYTES), "image/svg+xml")},
        )

    # Then
    assert response.status_code == 200
    assert response.headers["content-type"] == "image/png"
    assert response.content == fake_png
    conv.assert_called_once_with(TINY_SVG_BYTES, width=None, height=None, background="transparent")


def test_svg_to_png_forwards_width_height_and_background(client: TestClient) -> None:
    # When
    with patch.object(main, "_svg_to_png", return_value=b"png") as conv:
        response = client.post(
            "/svg-to-png",
            files={"file": ("input.svg", io.BytesIO(TINY_SVG_BYTES), "image/svg+xml")},
            data={"width": "512", "height": "256", "background": "#ffffff"},
        )

    # Then
    assert response.status_code == 200
    conv.assert_called_once_with(TINY_SVG_BYTES, width=512, height=256, background="#ffffff")


def test_svg_to_png_rejects_png_content_type(client: TestClient) -> None:
    # When  png hat einen anderen Content-Type als SVG, muss abgelehnt werden.
    response = client.post(
        "/svg-to-png",
        files={"file": ("input.png", io.BytesIO(TINY_PNG_BYTES), "image/png")},
    )

    # Then
    assert response.status_code == 415
    assert "Unsupported content type" in response.json()["detail"]


def test_svg_to_png_rejects_empty_file(client: TestClient) -> None:
    # When
    response = client.post(
        "/svg-to-png",
        files={"file": ("empty.svg", io.BytesIO(b""), "image/svg+xml")},
    )

    # Then
    assert response.status_code == 400
    assert response.json()["detail"] == "Empty file"


def test_svg_to_png_rejects_oversized_file(client: TestClient) -> None:
    # Given  knapp ueber MAX_BYTES.
    too_big = b"<svg/>" + b"x" * (main.MAX_BYTES)

    # When
    response = client.post(
        "/svg-to-png",
        files={"file": ("huge.svg", io.BytesIO(too_big), "image/svg+xml")},
    )

    # Then
    assert response.status_code == 413
    assert response.json()["detail"] == "File too large"


def test_svg_to_png_rejects_invalid_background_format(client: TestClient) -> None:
    # When  "red" ist kein erlaubtes Pattern (nur transparent oder #rrggbb).
    response = client.post(
        "/svg-to-png",
        files={"file": ("input.svg", io.BytesIO(TINY_SVG_BYTES), "image/svg+xml")},
        data={"background": "red"},
    )

    # Then  FastAPI/Pydantic liefert 422 fuer Pattern-Mismatch.
    assert response.status_code == 422


def test_svg_to_png_rejects_width_out_of_range(client: TestClient) -> None:
    # When
    response = client.post(
        "/svg-to-png",
        files={"file": ("input.svg", io.BytesIO(TINY_SVG_BYTES), "image/svg+xml")},
        data={"width": "0"},
    )

    # Then
    assert response.status_code == 422


def test_svg_to_png_wraps_cairosvg_exception_as_500(client: TestClient) -> None:
    # When
    with patch.object(main, "_svg_to_png", side_effect=ValueError("bad svg")):
        response = client.post(
            "/svg-to-png",
            files={"file": ("input.svg", io.BytesIO(TINY_SVG_BYTES), "image/svg+xml")},
        )

    # Then
    assert response.status_code == 500
    assert "SVG conversion failed" in response.json()["detail"]
    assert "ValueError" in response.json()["detail"]


def test_svg_to_png_invokes_cairosvg_with_bytestring_only_when_no_options() -> None:
    """Lazy-import-Wrapper darf nur die SVG-Bytes weitergeben, wenn keine Optionen gesetzt sind."""
    # Given
    fake_cairosvg = types.ModuleType("cairosvg")
    captured: dict[str, object] = {}

    def fake_svg2png(**kwargs: object) -> bytes:
        captured.update(kwargs)
        return b"fake-png"

    fake_cairosvg.svg2png = fake_svg2png  # type: ignore[attr-defined]

    # When
    with patch.dict("sys.modules", {"cairosvg": fake_cairosvg}):
        result = main._svg_to_png(b"<svg/>", width=None, height=None, background="transparent")

    # Then
    assert result == b"fake-png"
    assert captured == {"bytestring": b"<svg/>"}


def test_svg_to_png_invokes_cairosvg_with_all_options() -> None:
    """Mit explizitem width/height/background werden alle vier kwargs weitergegeben."""
    # Given
    fake_cairosvg = types.ModuleType("cairosvg")
    captured: dict[str, object] = {}

    def fake_svg2png(**kwargs: object) -> bytes:
        captured.update(kwargs)
        return b"fake-png"

    fake_cairosvg.svg2png = fake_svg2png  # type: ignore[attr-defined]

    # When
    with patch.dict("sys.modules", {"cairosvg": fake_cairosvg}):
        main._svg_to_png(b"<svg/>", width=64, height=32, background="#abcdef")

    # Then
    assert captured == {
        "bytestring": b"<svg/>",
        "output_width": 64,
        "output_height": 32,
        "background_color": "#abcdef",
    }


# ---------------------------------------------------------------------------
# /raster-to-png tests
# ---------------------------------------------------------------------------


def _palette_image_bytes() -> bytes:
    """PNG in Palette-Mode (P) — deckt den RGBA-Konvertierungspfad in _raster_to_png."""
    img = Image.new("P", (100, 100))
    img.putpalette([i for i in range(256)] * 3)
    buf = io.BytesIO()
    img.save(buf, format="PNG")
    return buf.getvalue()


def test_raster_to_png_converts_jpeg_to_png(client: TestClient) -> None:
    upload = _solid_image_bytes(200, 150, fmt="JPEG")

    response = client.post(
        "/raster-to-png",
        files={"file": ("photo.jpg", io.BytesIO(upload), "image/jpeg")},
    )

    assert response.status_code == 200
    assert response.headers["content-type"] == "image/png"
    out = _decode(response.content)
    assert out.format == "PNG"
    assert out.size == (200, 150)


def test_raster_to_png_resizes_when_both_dimensions_given(client: TestClient) -> None:
    upload = _solid_image_bytes(400, 300, fmt="PNG")

    response = client.post(
        "/raster-to-png",
        data={"width": "200", "height": "150"},
        files={"file": ("photo.png", io.BytesIO(upload), "image/png")},
    )

    assert response.status_code == 200
    out = _decode(response.content)
    assert out.size == (200, 150)


def test_raster_to_png_scales_proportionally_with_width_only(client: TestClient) -> None:
    upload = _solid_image_bytes(400, 200, fmt="PNG")

    response = client.post(
        "/raster-to-png",
        data={"width": "200"},
        files={"file": ("photo.png", io.BytesIO(upload), "image/png")},
    )

    assert response.status_code == 200
    out = _decode(response.content)
    assert out.size == (200, 100)


def test_raster_to_png_scales_proportionally_with_height_only(client: TestClient) -> None:
    upload = _solid_image_bytes(400, 200, fmt="PNG")

    response = client.post(
        "/raster-to-png",
        data={"height": "100"},
        files={"file": ("photo.png", io.BytesIO(upload), "image/png")},
    )

    assert response.status_code == 200
    out = _decode(response.content)
    assert out.size == (200, 100)


def test_raster_to_png_converts_palette_mode_image(client: TestClient) -> None:
    upload = _palette_image_bytes()

    response = client.post(
        "/raster-to-png",
        files={"file": ("palette.png", io.BytesIO(upload), "image/png")},
    )

    assert response.status_code == 200
    assert response.headers["content-type"] == "image/png"


def test_raster_to_png_returns_500_when_pillow_raises(
    client: TestClient, monkeypatch: pytest.MonkeyPatch
) -> None:
    def boom(_data: bytes, *, width: int | None, height: int | None) -> bytes:
        raise RuntimeError("pillow crashed")

    monkeypatch.setattr(main, "_raster_to_png", boom)
    upload = _solid_image_bytes(200, 150)

    response = client.post(
        "/raster-to-png",
        files={"file": ("photo.png", io.BytesIO(upload), "image/png")},
    )

    assert response.status_code == 500
    assert "Raster conversion failed" in response.json()["detail"]
