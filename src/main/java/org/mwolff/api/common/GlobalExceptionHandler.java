package org.mwolff.api.common;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * Allgemeiner Validations-Exception-Handler. Behandelt nur generische, fachfrei-anwendbare Faelle
 * (Bean Validation, Constraint Violations). Feature-spezifische Handler — z.B.
 * Tool-Verarbeitungsfehler — leben im jeweiligen Feature-Package (vgl. Review #58 P2.2, {@link
 * org.mwolff.api.tools.ToolExceptionHandler}).
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
    Map<String, Object> body = body(HttpStatus.BAD_REQUEST, "Validation failed");
    Map<String, String> fieldErrors = new LinkedHashMap<>();
    ex.getBindingResult()
        .getFieldErrors()
        .forEach(err -> fieldErrors.put(err.getField(), err.getDefaultMessage()));
    body.put("fieldErrors", fieldErrors);
    return ResponseEntity.badRequest().body(body);
  }

  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<Map<String, Object>> handleConstraintViolation(
      ConstraintViolationException ex) {
    Map<String, Object> body = body(HttpStatus.BAD_REQUEST, "Validation failed");
    Map<String, String> fieldErrors = new LinkedHashMap<>();
    for (ConstraintViolation<?> v : ex.getConstraintViolations()) {
      final String path = v.getPropertyPath().toString();
      final int dot = path.lastIndexOf('.');
      final String field = dot >= 0 ? path.substring(dot + 1) : path;
      fieldErrors.put(field, v.getMessage());
    }
    body.put("fieldErrors", fieldErrors);
    return ResponseEntity.badRequest().body(body);
  }

  /**
   * Springs Standard-Reaktion auf einen falsch typisierten Request-Parameter (z. B. {@code
   * ?includeArchived=yes} statt {@code true}/{@code false}) faellt sonst auf das generische
   * Default-Fehlerformat zurueck statt auf dieses API-eigene JSON-Format (Issue #297).
   */
  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public ResponseEntity<Map<String, Object>> handleTypeMismatch(
      MethodArgumentTypeMismatchException ex) {
    final String message =
        "Ungültiger Wert '%s' für Parameter '%s'".formatted(ex.getValue(), ex.getName());
    return ResponseEntity.badRequest().body(body(HttpStatus.BAD_REQUEST, message));
  }

  private Map<String, Object> body(HttpStatus status, String message) {
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("timestamp", Instant.now().toString());
    body.put("status", status.value());
    body.put("error", status.getReasonPhrase());
    body.put("message", message);
    return body;
  }
}
