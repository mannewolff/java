"""Tests fuer den FastAPI-Microservice (rembg ist gemockt)."""

from __future__ import annotations

import io
import types
from typing import Iterator
from unittest.mock import patch

import pytest
from fastapi.testclient import TestClient

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
    assert response.json()["detail"] == "Background removal failed"


def test_remove_bg_requires_file_field(client: TestClient) -> None:
    # When — FastAPI returns 422 for a missing required form field
    response = client.post("/remove-bg")

    # Then
    assert response.status_code == 422


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
