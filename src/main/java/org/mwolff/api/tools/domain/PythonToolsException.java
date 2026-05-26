package org.mwolff.api.tools.domain;

/**
 * Geworfen, wenn der python-tools-Dienst nicht erreichbar ist oder eine unerwartete Antwort
 * liefert. Wird vom Tool-Web-Layer auf HTTP 502 gemappt.
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
