package org.mwolff.api.image.domain;

/** Geworfen, wenn ein Bild-Upload fachlich invalide ist (leer, zu groß, falscher Typ) (#182). */
public class InvalidImageUploadException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  /** Stabiler Fehlercode für das Web-Mapping (EMPTY_FILE, UNSUPPORTED_TYPE, TOO_LARGE). */
  private final String code;

  public InvalidImageUploadException(final String code, final String message) {
    super(message);
    this.code = code;
  }

  public String code() {
    return code;
  }
}
