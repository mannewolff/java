package org.mwolff.api.tools;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

import org.apache.tika.Tika;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.TikaCoreProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

/**
 * Validates uploaded image files before they are forwarded to the python-tools microservice.
 *
 * <p>Trusting the client-declared Content-Type is unsafe (CLAUDE-security.md, code review P2-8).
 * This validator therefore checks the file size, ensures the file is non-empty and uses Apache Tika
 * to detect the actual MIME type from the byte signature.
 */
@Component
public class UploadValidator {

  /** Hard cap matching the spring.servlet.multipart.max-file-size default in application.yml. */
  static final long MAX_BYTES = 10L * 1024L * 1024L;

  static final Set<String> ALLOWED_MIME_TYPES = Set.of("image/png", "image/jpeg", "image/webp");

  private final Tika tika = new Tika();

  /**
   * Validates the uploaded image file.
   *
   * @throws InvalidUploadException if the upload is empty, too large or of an unsupported type
   */
  public void validateImageUpload(MultipartFile file) {
    if (file == null || file.isEmpty()) {
      throw new InvalidUploadException("EMPTY_FILE", "Uploaded file is empty.");
    }
    if (file.getSize() > MAX_BYTES) {
      throw new InvalidUploadException("FILE_TOO_LARGE", "Uploaded file exceeds the 10 MB limit.");
    }
    final String detected;
    try (InputStream in = file.getInputStream()) {
      final Metadata metadata = new Metadata();
      final String original = file.getOriginalFilename();
      if (original != null) {
        metadata.set(TikaCoreProperties.RESOURCE_NAME_KEY, original);
      }
      detected = tika.detect(in, metadata);
    } catch (IOException ex) {
      throw new InvalidUploadException("READ_FAILED", "Could not read uploaded file.");
    }
    if (!ALLOWED_MIME_TYPES.contains(detected)) {
      throw new InvalidUploadException(
          "UNSUPPORTED_FORMAT", "Unsupported file type. Allowed: PNG, JPEG, WebP.");
    }
  }
}
