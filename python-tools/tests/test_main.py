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
