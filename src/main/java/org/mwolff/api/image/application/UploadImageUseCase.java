package org.mwolff.api.image.application;

import java.util.Set;

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

  public UploadImageUseCase(final ImageRepository repository) {
    this.repository = repository;
  }

  @Transactional
  public StoredImage execute(final String contentType, final byte[] data) {
    if (data == null || data.length == 0) {
      throw new InvalidImageUploadException("EMPTY_FILE", "Uploaded file is empty.");
    }
    if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
      throw new InvalidImageUploadException(
          "UNSUPPORTED_TYPE", "Unsupported image type: " + contentType);
    }
    if (data.length > MAX_SIZE_BYTES) {
      throw new InvalidImageUploadException(
          "TOO_LARGE", "Image exceeds the 5 MB limit (" + data.length + " bytes).");
    }
    return repository.save(StoredImage.of(contentType, data));
  }
}
