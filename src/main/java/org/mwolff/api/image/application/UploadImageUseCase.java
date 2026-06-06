package org.mwolff.api.image.application;

import java.util.Set;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.tika.Tika;
import org.mwolff.api.image.domain.ImageRepository;
import org.mwolff.api.image.domain.InvalidImageUploadException;
import org.mwolff.api.image.domain.StoredImage;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Validiert und speichert einen Bild-Upload (#182). */
@Component
public class UploadImageUseCase {

  /** Erlaubte Bild-MIME-Typen. */
  static final Set<String> ALLOWED_CONTENT_TYPES =
      Set.of("image/jpeg", "image/png", "image/webp", "image/gif");

  /** Maximale Dateigröße: 5 MB. */
  static final int MAX_SIZE_BYTES = 5 * 1024 * 1024;

  private final ImageRepository repository;

  // Tika.detect(byte[], String) liest die Magic-Bytes direkt aus dem Array (keine IO) und ist
  // thread-safe — als Feld wiederverwendbar (Muster TikaUploadValidator, #231).
  private final Tika tika = new Tika();

  public UploadImageUseCase(final ImageRepository repository) {
    this.repository = repository;
  }

  /**
   * Validiert die Bytes per Magic-Bytes (Tika) und speichert das Bild. Der client-gemeldete
   * Content-Type wird bewusst NICHT vertraut (#231) — der gespeicherte {@code contentType} stammt
   * aus der Byte-Detektion. {@code filename} dient Tika nur als zusätzlicher Hint.
   */
  @Transactional
  public StoredImage execute(final String userSub, final byte[] data, final String filename) {
    if (data == null || data.length == 0) {
      throw new InvalidImageUploadException("EMPTY_FILE", "Uploaded file is empty.");
    }
    if (data.length > MAX_SIZE_BYTES) {
      throw new InvalidImageUploadException(
          "TOO_LARGE", "Image exceeds the 5 MB limit (" + data.length + " bytes).");
    }
    final String detected = tika.detect(data, filename);
    if (!ALLOWED_CONTENT_TYPES.contains(detected)) {
      throw new InvalidImageUploadException(
          "UNSUPPORTED_TYPE", "Unsupported image type: " + detected);
    }
    return repository.save(StoredImage.of(userSub, detected, data, sha256Hex(data)));
  }

  /**
   * SHA-256 der Binärdaten als Hex-String (#199) — Basis für die Duplikat-Erkennung. Nutzt
   * commons-codec (kein checked NoSuchAlgorithmException, daher kein unerreichbarer catch-Zweig);
   * identische lowercase-Hex-Ausgabe wie zuvor (#232).
   */
  static String sha256Hex(final byte[] data) {
    return DigestUtils.sha256Hex(data);
  }
}
