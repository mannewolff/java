package org.mwolff.api.ingest.domain;

/**
 * Wird geworfen, wenn ein Token-Plaintext kein gueltiges Mapping in der DB findet — entweder weil
 * der Token nie existierte, oder weil er widerrufen wurde. In beiden Faellen reicht der Filter eine
 * 401 zurueck.
 */
public class InvalidIngestTokenException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public InvalidIngestTokenException(String message) {
    super(message);
  }
}
