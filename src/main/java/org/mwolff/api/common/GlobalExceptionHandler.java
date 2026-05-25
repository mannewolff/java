package org.mwolff.api.common;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;

import org.mwolff.api.tools.PythonToolsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger LOG = LoggerFactory.getLogger(GlobalExceptionHandler.class);
  private static final String UPSTREAM_GENERIC_MESSAGE = "Tool-Service derzeit nicht erreichbar.";

  @ExceptionHandler(PythonToolsException.class)
  public ResponseEntity<Map<String, Object>> handlePythonTools(PythonToolsException ex) {
    // Internal log carries the full diagnostic; external response stays generic so we do not
    // leak upstream details (CLAUDE-security.md, see also code review P2-7).
    LOG.warn("python-tools upstream failure: {}", ex.getMessage(), ex);
    return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
        .body(body(HttpStatus.BAD_GATEWAY, UPSTREAM_GENERIC_MESSAGE));
  }

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

  private Map<String, Object> body(HttpStatus status, String message) {
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("timestamp", Instant.now().toString());
    body.put("status", status.value());
    body.put("error", status.getReasonPhrase());
    body.put("message", message);
    return body;
  }
}
