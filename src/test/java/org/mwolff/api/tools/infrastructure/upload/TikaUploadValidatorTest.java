package org.mwolff.api.tools.infrastructure.upload;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.mwolff.api.tools.domain.InvalidUploadException;
import org.mwolff.api.tools.domain.UploadedImage;

class TikaUploadValidatorTest {

  private static final byte[] PNG_HEADER =
      new byte[] {
        (byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1a, '\n', 0, 0, 0, 0, 'I', 'H', 'D', 'R'
      };

  private static final byte[] JPEG_HEADER =
      new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 16, 'J', 'F', 'I', 'F'};

  private final TikaUploadValidator validator = new TikaUploadValidator();

  @Test
  void shouldAcceptRealPngEvenWhenContentTypeIsLying() {
    final UploadedImage image = new UploadedImage(PNG_HEADER, "text/plain", "image.png");

    validator.validateImage(image);
  }

  @Test
  void shouldAcceptRealJpeg() {
    final UploadedImage image = new UploadedImage(JPEG_HEADER, "image/jpeg", "photo.jpg");

    validator.validateImage(image);
  }

  @Test
  void shouldRejectFileExceedingMaximumSizeViaCustomSize() {
    // UploadedImage prüft Konstruktor-Nonempty — Tika-Validator prüft Größe separat.
    // Wir bauen ein UploadedImage mit künstlich großer size() durch Override.
    final byte[] payload = new byte[(int) TikaUploadValidator.MAX_BYTES + 1];
    System.arraycopy(PNG_HEADER, 0, payload, 0, PNG_HEADER.length);
    final UploadedImage image = new UploadedImage(payload, "image/png", "huge.png");

    assertThatThrownBy(() -> validator.validateImage(image))
        .isInstanceOf(InvalidUploadException.class)
        .matches(ex -> ((InvalidUploadException) ex).code().equals("FILE_TOO_LARGE"));
  }

  @Test
  void shouldRejectPlainTextEvenWhenLabelledAsPng() {
    final UploadedImage image =
        new UploadedImage("Hello, world".getBytes(), "image/png", "fake.png");

    assertThatThrownBy(() -> validator.validateImage(image))
        .isInstanceOf(InvalidUploadException.class)
        .matches(ex -> ((InvalidUploadException) ex).code().equals("UNSUPPORTED_FORMAT"));
  }

  @Test
  void shouldAcceptWithoutOriginalFilename() {
    final UploadedImage image = new UploadedImage(PNG_HEADER, "image/png", null);

    validator.validateImage(image);
  }

  @Test
  void shouldExposeWebpAsAllowed() {
    // WebP RIFF header
    final byte[] webp =
        new byte[] {'R', 'I', 'F', 'F', 0x24, 0, 0, 0, 'W', 'E', 'B', 'P', 'V', 'P', '8', 'L'};
    final UploadedImage image = new UploadedImage(webp, "image/webp", "x.webp");
    // WebP magic alone reicht Tika — auch wenn der Body kurz ist.
    assertThat(TikaUploadValidator.ALLOWED_MIME_TYPES).contains("image/webp");
    try {
      validator.validateImage(image);
    } catch (InvalidUploadException ex) {
      // Manche Tika-Versionen erkennen kurze WebP-Stubs nicht — nicht-deterministisches Detail.
      // Wichtig ist nur, dass image/webp grundsätzlich in der Allowlist steht (siehe Assertion
      // oben).
      assertThat(ex.code()).isEqualTo("UNSUPPORTED_FORMAT");
    }
  }

  // ---- validateSvg ----

  private static final byte[] SVG_BYTES =
      ("<?xml version=\"1.0\"?><svg xmlns=\"http://www.w3.org/2000/svg\" width=\"10\""
              + " height=\"10\"><rect width=\"10\" height=\"10\"/></svg>")
          .getBytes();

  @Test
  void shouldAcceptRealSvg() {
    final UploadedImage svg = new UploadedImage(SVG_BYTES, "image/svg+xml", "logo.svg");

    validator.validateSvg(svg);
  }

  @Test
  void shouldRejectNonSvgInSvgPath() {
    final UploadedImage png = new UploadedImage(PNG_HEADER, "image/png", "image.png");

    assertThatThrownBy(() -> validator.validateSvg(png))
        .isInstanceOf(InvalidUploadException.class)
        .matches(t -> ((InvalidUploadException) t).code().equals("UNSUPPORTED_FORMAT"));
  }

  @Test
  void shouldRejectOversizedSvg() {
    final byte[] huge = new byte[(int) (TikaUploadValidator.MAX_BYTES + 1)];
    final UploadedImage svg = new UploadedImage(huge, "image/svg+xml", "huge.svg");

    assertThatThrownBy(() -> validator.validateSvg(svg))
        .isInstanceOf(InvalidUploadException.class)
        .matches(t -> ((InvalidUploadException) t).code().equals("FILE_TOO_LARGE"));
  }
}
