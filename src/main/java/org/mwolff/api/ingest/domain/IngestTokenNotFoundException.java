package org.mwolff.api.ingest.domain;

/**
 * Wird geworfen, wenn ein Token nicht existiert ODER nicht dem aufrufenden User gehoert. Wie bei
 * Dashboard/TimeSeries werden beide Faelle als 404 nach aussen gemappt (kein Existenz-Leak).
 */
public class IngestTokenNotFoundException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public IngestTokenNotFoundException(long id) {
    super("IngestToken " + id + " not found");
  }
}
