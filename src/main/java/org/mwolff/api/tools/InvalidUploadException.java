package org.mwolff.api.tools;

/** Thrown when an uploaded file fails validation (empty, too large, wrong type, etc.). */
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
