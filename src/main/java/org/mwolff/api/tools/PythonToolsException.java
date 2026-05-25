package org.mwolff.api.tools;

/**
 * Wird geworfen, wenn der python-tools-Service nicht erreichbar ist oder eine unerwartete Antwort
 * liefert. Wird vom GlobalExceptionHandler auf HTTP 502 gemappt.
 */
public class PythonToolsException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public PythonToolsException(String message) {
    super(message);
  }

  public PythonToolsException(String message, Throwable cause) {
    super(message, cause);
  }
}
