package org.mwolff.api.tools.infrastructure.upload;

import java.util.Set;

import org.apache.tika.Tika;
import org.mwolff.api.tools.domain.InvalidUploadException;
import org.mwolff.api.tools.domain.UploadValidatorPort;
import org.mwolff.api.tools.domain.UploadedImage;
import org.mwolff.api.tools.domain.ValidatedImage;
import org.springframework.stereotype.Component;

/**
 * Tika-basierte Implementierung von {@link UploadValidatorPort}. Prüft Größe und tatsächlichen
 * MIME-Type (anhand der Byte-Signatur, nicht des client-gemeldeten Content-Type — letzteres ist
 * nicht vertrauenswürdig).
 */
@Component
public class TikaUploadValidator implements UploadValidatorPort {

  /**
   * Harte Obergrenze, passt zu {@code spring.servlet.multipart.max-file-size} in application.yml.
   */
  static final long MAX_BYTES = 10L * 1024L * 1024L;

  static final Set<String> ALLOWED_MIME_TYPES = Set.of("image/png", "image/jpeg", "image/webp");

  static final String SVG_MIME_TYPE = "image/svg+xml";

  private final Tika tika = new Tika();

  @Override
  public ValidatedImage validateImage(UploadedImage image) {
    enforceSize(image);
    // Tika.detect(byte[], String) liest Magic-Bytes direkt aus dem Array — keine IO,
    // also auch kein READ_FAILED-Pfad mehr (im UploadedImageMapper wird beim Lesen aus
    // MultipartFile auf IO-Fehler geprueft, vor wir hier landen).
    final String detected = tika.detect(image.bytes(), image.originalFilename());
    if (!ALLOWED_MIME_TYPES.contains(detected)) {
      throw new InvalidUploadException(
          "UNSUPPORTED_FORMAT", "Unsupported file type. Allowed: PNG, JPEG, WebP.");
    }
    return new ValidatedImage(image.bytes(), detected, image.originalFilename());
  }

  @Override
  public ValidatedImage validateSvg(UploadedImage image) {
    enforceSize(image);
    final String detected = tika.detect(image.bytes(), image.originalFilename());
    if (!SVG_MIME_TYPE.equals(detected)) {
      throw new InvalidUploadException(
          "UNSUPPORTED_FORMAT", "Unsupported file type. Expected SVG (image/svg+xml).");
    }
    return new ValidatedImage(image.bytes(), detected, image.originalFilename());
  }

  private static void enforceSize(UploadedImage image) {
    if (image.size() > MAX_BYTES) {
      throw new InvalidUploadException("FILE_TOO_LARGE", "Uploaded file exceeds the 10 MB limit.");
    }
  }
}
