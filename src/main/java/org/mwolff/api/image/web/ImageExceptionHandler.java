package org.mwolff.api.image.web;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import org.mwolff.api.image.domain.ImageNotFoundException;
import org.mwolff.api.image.domain.InvalidImageUploadException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Image-spezifischer Exception-Handler, paket-skopiert auf {@code org.mwolff.api.image.web.**}.
 * Mapped fachliche Fehler auf HTTP-Status (#182).
 */
@RestControllerAdvice(basePackages = "org.mwolff.api.image.web")
public class ImageExceptionHandler {

  @ExceptionHandler(ImageNotFoundException.class)
  public ResponseEntity<Map<String, Object>> handleNotFound(final ImageNotFoundException ex) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(body(HttpStatus.NOT_FOUND, ex.getMessage(), null));
  }

  @ExceptionHandler(InvalidImageUploadException.class)
  public ResponseEntity<Map<String, Object>> handleInvalidUpload(
      final InvalidImageUploadException ex) {
    final HttpStatus status = statusFor(ex.code());
    return ResponseEntity.status(status).body(body(status, ex.getMessage(), ex.code()));
  }

  private static HttpStatus statusFor(final String code) {
    return switch (code) {
      case "UNSUPPORTED_TYPE" -> HttpStatus.UNSUPPORTED_MEDIA_TYPE;
      case "TOO_LARGE" -> HttpStatus.PAYLOAD_TOO_LARGE;
      default -> HttpStatus.BAD_REQUEST;
    };
  }

  private Map<String, Object> body(
      final HttpStatus status, final String message, final String code) {
    final Map<String, Object> body = new LinkedHashMap<>();
    body.put("timestamp", Instant.now().toString());
    body.put("status", status.value());
    body.put("error", status.getReasonPhrase());
    body.put("message", message);
    if (code != null) {
      body.put("code", code);
    }
    return body;
  }
}
