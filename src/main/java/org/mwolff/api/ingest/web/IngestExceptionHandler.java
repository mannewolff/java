package org.mwolff.api.ingest.web;

import org.mwolff.api.ingest.domain.IngestTokenNotFoundException;
import org.mwolff.api.timeseries.domain.TimeSeriesNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Mapping der Ingest-Domain-Exceptions auf HTTP-Antworten. Paket-skopiert — greift nur fuer die
 * beiden Ingest-Controller. {@link TimeSeriesNotFoundException} wird vom {@link
 * org.mwolff.api.ingest.web.IngestController} indirekt ausgeloest (foreign timeSeriesId via
 * Token-User), darum hier mit gemappt — der dedizierte TimeSeries-Handler reagiert nicht auf
 * Requests in diesem Paket.
 */
@RestControllerAdvice(basePackages = "org.mwolff.api.ingest.web")
public class IngestExceptionHandler {

  @ExceptionHandler(IngestTokenNotFoundException.class)
  public ResponseEntity<Void> handleTokenNotFound(IngestTokenNotFoundException ex) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
  }

  @ExceptionHandler(TimeSeriesNotFoundException.class)
  public ResponseEntity<Void> handleSeriesNotFound(TimeSeriesNotFoundException ex) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<String> handleIllegalArgument(IllegalArgumentException ex) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
  }
}
