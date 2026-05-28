package org.mwolff.api.ingest.web;

import jakarta.validation.Valid;

import org.mwolff.api.ingest.web.dto.IngestEntryRequest;
import org.mwolff.api.timeseries.application.AddEntryUseCase;
import org.mwolff.api.timeseries.domain.TimeSeriesEntry;
import org.mwolff.api.timeseries.web.dto.TimeSeriesEntryResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Oeffentlicher Ingest-Endpoint. Auth via {@code X-Ingest-Token}-Header — die {@link
 * IngestTokenAuthFilter}-Kette setzt den authentifizierten User-Sub vor diesem Controller.
 */
@RestController
@RequestMapping("/api/ingest")
public class IngestController {

  private final AddEntryUseCase addEntryUseCase;

  public IngestController(AddEntryUseCase addEntryUseCase) {
    this.addEntryUseCase = addEntryUseCase;
  }

  @PostMapping
  public ResponseEntity<TimeSeriesEntryResponse> ingest(
      @AuthenticationPrincipal String userSub, @Valid @RequestBody IngestEntryRequest body) {
    final TimeSeriesEntry created =
        addEntryUseCase.execute(userSub, body.timeSeriesId(), body.timestamp(), body.value());
    return ResponseEntity.status(HttpStatus.CREATED).body(TimeSeriesEntryResponse.from(created));
  }
}
