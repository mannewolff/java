package org.mwolff.api.ingest.web;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.mwolff.api.ingest.domain.IngestTokenNotFoundException;
import org.mwolff.api.timeseries.domain.TimeSeriesNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class IngestExceptionHandlerTest {

  private final IngestExceptionHandler handler = new IngestExceptionHandler();

  @Test
  void mapsIngestTokenNotFoundTo404() {
    final ResponseEntity<Void> response =
        handler.handleTokenNotFound(new IngestTokenNotFoundException(42L));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
  }

  @Test
  void mapsTimeSeriesNotFoundTo404() {
    final ResponseEntity<Void> response =
        handler.handleSeriesNotFound(new TimeSeriesNotFoundException(99L));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
  }

  @Test
  void mapsIllegalArgumentTo400WithMessage() {
    final ResponseEntity<String> response =
        handler.handleIllegalArgument(new IllegalArgumentException("bad value"));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody()).isEqualTo("bad value");
  }
}
