package org.mwolff.api.dashboard.web;

import org.mwolff.api.dashboard.domain.DashboardNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Mapping von Dashboard-Domain-Exceptions auf HTTP-Antworten. Paket-skopiert, damit es nur für
 * Dashboard-Controller greift und nicht andere Komponenten überstimmt.
 */
@RestControllerAdvice(basePackages = "org.mwolff.api.dashboard.web")
public class DashboardExceptionHandler {

  @ExceptionHandler(DashboardNotFoundException.class)
  public ResponseEntity<Void> handleNotFound(DashboardNotFoundException ex) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
  }
}
