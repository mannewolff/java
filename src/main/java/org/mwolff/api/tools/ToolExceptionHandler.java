package org.mwolff.api.tools;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Tool-spezifischer Exception-Handler. Lebt im tools-Package, damit common-Code nicht auf konkrete
 * Tool-Exceptions zugreifen muss (vgl. Review #58 P2.2). Der ControllerAdvice ist paket-skopiert,
 * sodass Handler nur fuer Controller in {@code org.mwolff.api.tools.**} greifen — andere
 * Komponenten bleiben unberuehrt vom generischen Mapping.
 */
@RestControllerAdvice(basePackages = "org.mwolff.api.tools")
public class ToolExceptionHandler {

  private static final Logger LOG = LoggerFactory.getLogger(ToolExceptionHandler.class);
  private static final String UPSTREAM_GENERIC_MESSAGE = "Tool-Service derzeit nicht erreichbar.";

  @ExceptionHandler(PythonToolsException.class)
  public ResponseEntity<Map<String, Object>> handlePythonTools(PythonToolsException ex) {
    // Internal log carries the full diagnostic; external response stays generic so we do not
    // leak upstream details (CLAUDE-security.md, see also code review P2-7).
    LOG.warn("python-tools upstream failure: {}", ex.getMessage(), ex);
    return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
        .body(body(HttpStatus.BAD_GATEWAY, UPSTREAM_GENERIC_MESSAGE));
  }

  @ExceptionHandler(InvalidUploadException.class)
  public ResponseEntity<Map<String, Object>> handleInvalidUpload(InvalidUploadException ex) {
    final Map<String, Object> body = body(HttpStatus.BAD_REQUEST, ex.getMessage());
    body.put("code", ex.code());
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
