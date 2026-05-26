package org.mwolff.api.tools.web;

import java.io.IOException;

import org.mwolff.api.tools.domain.InvalidUploadException;
import org.mwolff.api.tools.domain.UploadedImage;
import org.springframework.web.multipart.MultipartFile;

/**
 * Wandelt Spring's {@link MultipartFile} in den Domain-Record {@link UploadedImage}.
 *
 * <p>Lebt im web-Package, damit MultipartFile niemals in Application- oder Domain-Code leakt. Ein
 * leerer Upload führt zu {@link InvalidUploadException} — das gleiche Verhalten wie der frühere
 * Tika-Validator vor dem Refactor.
 */
final class UploadedImageMapper {

  private UploadedImageMapper() {
    // Utility class
  }

  static UploadedImage toDomain(MultipartFile file) {
    if (file == null || file.isEmpty()) {
      throw new InvalidUploadException("EMPTY_FILE", "Uploaded file is empty.");
    }
    final byte[] bytes;
    try {
      bytes = file.getBytes();
    } catch (IOException ex) {
      throw new InvalidUploadException("READ_FAILED", "Could not read uploaded file.");
    }
    return new UploadedImage(bytes, file.getContentType(), file.getOriginalFilename());
  }
}
