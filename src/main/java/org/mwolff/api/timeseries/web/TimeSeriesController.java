package org.mwolff.api.timeseries.web;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;

import org.mwolff.api.timeseries.application.AddEntryUseCase;
import org.mwolff.api.timeseries.application.AggregateTimeSeriesUseCase;
import org.mwolff.api.timeseries.application.BulkAddEntriesUseCase;
import org.mwolff.api.timeseries.application.CreateTimeSeriesUseCase;
import org.mwolff.api.timeseries.application.DeleteTimeSeriesUseCase;
import org.mwolff.api.timeseries.application.GetLatestEntryUseCase;
import org.mwolff.api.timeseries.application.GetTimeSeriesUseCase;
import org.mwolff.api.timeseries.application.ListEntriesUseCase;
import org.mwolff.api.timeseries.application.ListTimeSeriesUseCase;
import org.mwolff.api.timeseries.application.UpdateTimeSeriesUseCase;
import org.mwolff.api.timeseries.domain.Granularity;
import org.mwolff.api.timeseries.domain.TimeSeries;
import org.mwolff.api.timeseries.domain.TimeSeriesEntry;
import org.mwolff.api.timeseries.web.dto.AddEntryRequest;
import org.mwolff.api.timeseries.web.dto.AggregateBucketResponse;
import org.mwolff.api.timeseries.web.dto.BulkImportResponse;
import org.mwolff.api.timeseries.web.dto.BulkImportResponse.BulkRowError;
import org.mwolff.api.timeseries.web.dto.CreateTimeSeriesRequest;
import org.mwolff.api.timeseries.web.dto.TimeSeriesEntryResponse;
import org.mwolff.api.timeseries.web.dto.TimeSeriesSummaryResponse;
import org.mwolff.api.timeseries.web.dto.UpdateTimeSeriesRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST-Adapter fuer Zeitreihen. Alle Endpoints sind durch {@code
 * SecurityConfig#requestMatchers("/api/timeseries/**").hasRole("USER")} geschuetzt. Owner-Check
 * passiert in den Use-Cases — der Controller leitet nur {@code sub} aus dem JWT weiter.
 */
@Validated
@RestController
@RequestMapping("/api/timeseries")
public class TimeSeriesController {

  private final ListTimeSeriesUseCase listUseCase;
  private final CreateTimeSeriesUseCase createUseCase;
  private final GetTimeSeriesUseCase getUseCase;
  private final UpdateTimeSeriesUseCase updateUseCase;
  private final DeleteTimeSeriesUseCase deleteUseCase;
  private final AddEntryUseCase addEntryUseCase;
  private final ListEntriesUseCase listEntriesUseCase;
  private final AggregateTimeSeriesUseCase aggregateUseCase;
  private final GetLatestEntryUseCase latestEntryUseCase;
  private final BulkAddEntriesUseCase bulkUseCase;

  /** 5 MiB Hardlimit fuer Bulk-Body — schuetzt vor versehentlichem 100-MB-Upload. */
  static final int MAX_BULK_BYTES = 5 * 1024 * 1024;

  /** Size-Guard als reine Methode — direkt am Grenzwert testbar statt per 5-MiB-Payload. */
  static boolean isBulkBodyTooLarge(final int length) {
    return length > MAX_BULK_BYTES;
  }

  public TimeSeriesController(
      ListTimeSeriesUseCase listUseCase,
      CreateTimeSeriesUseCase createUseCase,
      GetTimeSeriesUseCase getUseCase,
      UpdateTimeSeriesUseCase updateUseCase,
      DeleteTimeSeriesUseCase deleteUseCase,
      AddEntryUseCase addEntryUseCase,
      ListEntriesUseCase listEntriesUseCase,
      AggregateTimeSeriesUseCase aggregateUseCase,
      GetLatestEntryUseCase latestEntryUseCase,
      BulkAddEntriesUseCase bulkUseCase) {
    this.listUseCase = listUseCase;
    this.createUseCase = createUseCase;
    this.getUseCase = getUseCase;
    this.updateUseCase = updateUseCase;
    this.deleteUseCase = deleteUseCase;
    this.addEntryUseCase = addEntryUseCase;
    this.listEntriesUseCase = listEntriesUseCase;
    this.aggregateUseCase = aggregateUseCase;
    this.latestEntryUseCase = latestEntryUseCase;
    this.bulkUseCase = bulkUseCase;
  }

  @GetMapping
  public List<TimeSeriesSummaryResponse> list(JwtAuthenticationToken auth) {
    return listUseCase.execute(auth.getToken().getSubject()).stream()
        .map(TimeSeriesSummaryResponse::from)
        .toList();
  }

  @PostMapping
  public ResponseEntity<TimeSeriesSummaryResponse> create(
      JwtAuthenticationToken auth, @Valid @RequestBody CreateTimeSeriesRequest body) {
    final TimeSeries created =
        createUseCase.execute(
            auth.getToken().getSubject(),
            body.name(),
            body.description(),
            body.unit(),
            body.dataType());
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(TimeSeriesSummaryResponse.from(created, 0L));
  }

  @GetMapping("/{id}")
  public TimeSeriesSummaryResponse get(JwtAuthenticationToken auth, @PathVariable @Min(1) long id) {
    return TimeSeriesSummaryResponse.from(getUseCase.execute(auth.getToken().getSubject(), id));
  }

  @PutMapping("/{id}")
  public TimeSeriesSummaryResponse update(
      JwtAuthenticationToken auth,
      @PathVariable @Min(1) long id,
      @Valid @RequestBody UpdateTimeSeriesRequest body) {
    final String sub = auth.getToken().getSubject();
    updateUseCase.execute(sub, id, body.name(), body.description(), body.unit(), body.dataType());
    return TimeSeriesSummaryResponse.from(getUseCase.execute(sub, id));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(JwtAuthenticationToken auth, @PathVariable @Min(1) long id) {
    deleteUseCase.execute(auth.getToken().getSubject(), id);
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/{id}/entries")
  public List<TimeSeriesEntryResponse> listEntries(
      JwtAuthenticationToken auth,
      @PathVariable @Min(1) long id,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
          Instant from,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
          Instant to,
      @RequestParam(required = false) Integer limit) {
    return listEntriesUseCase
        .execute(
            auth.getToken().getSubject(),
            id,
            Optional.ofNullable(from),
            Optional.ofNullable(to),
            Optional.ofNullable(limit))
        .stream()
        .map(TimeSeriesEntryResponse::from)
        .toList();
  }

  @PostMapping("/{id}/entries")
  public ResponseEntity<TimeSeriesEntryResponse> addEntry(
      JwtAuthenticationToken auth,
      @PathVariable @Min(1) long id,
      @Valid @RequestBody AddEntryRequest body) {
    final TimeSeriesEntry created =
        addEntryUseCase.execute(auth.getToken().getSubject(), id, body.timestamp(), body.value());
    return ResponseEntity.status(HttpStatus.CREATED).body(TimeSeriesEntryResponse.from(created));
  }

  @GetMapping("/{id}/latest")
  public TimeSeriesEntryResponse latest(
      JwtAuthenticationToken auth, @PathVariable @Min(1) long id) {
    return TimeSeriesEntryResponse.from(
        latestEntryUseCase.execute(auth.getToken().getSubject(), id));
  }

  @PostMapping(path = "/{id}/entries/bulk", consumes = "text/csv")
  public ResponseEntity<BulkImportResponse> bulkImport(
      JwtAuthenticationToken auth, @PathVariable @Min(1) long id, @RequestBody byte[] body) {
    if (isBulkBodyTooLarge(body.length)) {
      throw new IllegalArgumentException("body too large: max " + MAX_BULK_BYTES + " bytes");
    }
    final String csv = new String(body, java.nio.charset.StandardCharsets.UTF_8);
    final CsvBulkParser.ParseResult parsed = CsvBulkParser.parse(csv);
    if (parsed.hasErrors()) {
      final List<BulkRowError> mapped =
          parsed.errors().stream().map(e -> new BulkRowError(e.line(), e.reason())).toList();
      return ResponseEntity.badRequest().body(new BulkImportResponse(0, mapped));
    }
    final int inserted = bulkUseCase.execute(auth.getToken().getSubject(), id, parsed.rows());
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(new BulkImportResponse(inserted, List.of()));
  }

  @GetMapping("/{id}/aggregate")
  public List<AggregateBucketResponse> aggregate(
      JwtAuthenticationToken auth,
      @PathVariable @Min(1) long id,
      @RequestParam Granularity granularity,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
          Instant from,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
          Instant to,
      @RequestParam(required = false) Integer limit) {
    return aggregateUseCase
        .execute(
            auth.getToken().getSubject(),
            id,
            granularity,
            Optional.ofNullable(from),
            Optional.ofNullable(to),
            Optional.ofNullable(limit))
        .stream()
        .map(AggregateBucketResponse::from)
        .toList();
  }
}
