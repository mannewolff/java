package org.mwolff.api.timeseries.web;

import org.mwolff.api.timeseries.domain.TimeSeriesNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Mapping von TimeSeries-Domain-Exceptions auf HTTP-Antworten. Paket-skopiert, damit es nur fuer
 * TimeSeries-Controller greift.
 */
@RestControllerAdvice(basePackages = "org.mwolff.api.timeseries.web")
public class TimeSeriesExceptionHandler {

  @ExceptionHandler(TimeSeriesNotFoundException.class)
  public ResponseEntity<Void> handleNotFound(TimeSeriesNotFoundException ex) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<String> handleIllegalArgument(IllegalArgumentException ex) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
  }
}
