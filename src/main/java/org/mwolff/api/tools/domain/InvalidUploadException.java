package org.mwolff.api.tools.domain;

/** Geworfen, wenn ein Upload fachlich invalide ist (leer, zu groß, falscher Typ, …). */
public class InvalidUploadException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  private final String code;

  public InvalidUploadException(String code, String message) {
    super(message);
    this.code = code;
  }

  public String code() {
    return code;
  }
}
