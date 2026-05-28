package org.mwolff.api.timeseries.web;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.mwolff.api.timeseries.application.BulkAddEntriesUseCase.BulkEntry;

/**
 * Parser fuer den CSV-Bulk-Import. Erkennt Header-Zeilen heuristisch (erste Zeile keine parsbare
 * Timestamp/Value-Kombination) und akzeptiert mehrere Timestamp-Formate.
 *
 * <p>Erlaubte Formate fuer den Timestamp:
 *
 * <ul>
 *   <li>ISO-8601 mit Z ({@code 2026-05-27T12:00:00Z})
 *   <li>{@code yyyy-MM-dd HH:mm:ss} (UTC interpretiert)
 *   <li>{@code yyyy-MM-dd} (UTC, 12:00 als Default-Tageszeit)
 * </ul>
 *
 * <p>Erlaubt nur Komma als Trennzeichen — Semikolon, Tab oder Pipe werfen direkt.
 */
final class CsvBulkParser {

  /** Parser-Ergebnis: bei Erfolg die Zeilen, bei Fehler die Liste der Issues. */
  record ParseResult(List<BulkEntry> rows, List<RowError> errors) {
    boolean hasErrors() {
      return !errors.isEmpty();
    }
  }

  /** Einzel-Zeile-Fehler. Zeilennummer ist 1-basiert und beruecksichtigt den optionalen Header. */
  record RowError(long line, String reason) {}

  /** Hartes Maximum, das ueber den UseCase noch einmal verifiziert wird. */
  static final int MAX_ROWS = 50_000;

  private CsvBulkParser() {}

  static ParseResult parse(String csv) {
    final List<BulkEntry> rows = new ArrayList<>();
    final List<RowError> errors = new ArrayList<>();
    try (Reader reader = new StringReader(csv);
        CSVParser parser =
            CSVFormat.DEFAULT.builder().setIgnoreEmptyLines(true).build().parse(reader)) {
      boolean first = true;
      for (CSVRecord record : parser) {
        final long line = record.getRecordNumber();
        if (first) {
          first = false;
          if (looksLikeHeader(record)) {
            continue;
          }
        }
        if (rows.size() >= MAX_ROWS) {
          errors.add(new RowError(line, "max " + MAX_ROWS + " rows exceeded"));
          break;
        }
        if (record.size() < 2) {
          errors.add(
              new RowError(line, "expected 2 columns (timestamp,value), found " + record.size()));
          continue;
        }
        if (record.size() > 2) {
          errors.add(
              new RowError(
                  line,
                  "expected 2 columns (timestamp,value), found "
                      + record.size()
                      + " — check Trennzeichen (Komma, kein Semikolon)"));
          continue;
        }
        final String tsRaw = record.get(0).trim();
        final String valueRaw = record.get(1).trim();
        if (valueRaw.contains(",")) {
          errors.add(new RowError(line, "value: use '.' as decimal separator, not ','"));
          continue;
        }
        final Instant ts;
        try {
          ts = parseTimestamp(tsRaw);
        } catch (DateTimeParseException ex) {
          errors.add(new RowError(line, "invalid timestamp: " + tsRaw));
          continue;
        }
        final BigDecimal value;
        try {
          value = new BigDecimal(valueRaw);
        } catch (NumberFormatException ex) {
          errors.add(new RowError(line, "invalid value: " + valueRaw));
          continue;
        }
        rows.add(new BulkEntry(ts, value));
      }
    } catch (IOException ex) {
      // StringReader kann keine IOException werfen, aber Compiler will den Catch.
      errors.add(new RowError(0, "io error: " + ex.getMessage()));
    }
    return new ParseResult(rows, errors);
  }

  private static boolean looksLikeHeader(CSVRecord record) {
    if (record.size() < 1) {
      return false;
    }
    final String first = record.get(0).trim();
    // Wenn das erste Feld parsbar als Instant/Datum ist, war's kein Header.
    try {
      parseTimestamp(first);
      return false;
    } catch (DateTimeParseException ex) {
      return true;
    }
  }

  private static Instant parseTimestamp(String raw) {
    if (raw.contains("T") || raw.endsWith("Z")) {
      return Instant.parse(raw);
    }
    if (raw.length() == 10) {
      final LocalDate date = LocalDate.parse(raw);
      return date.atTime(12, 0).toInstant(ZoneOffset.UTC);
    }
    final LocalDateTime ldt = LocalDateTime.parse(raw.replace(' ', 'T'));
    return ldt.toInstant(ZoneOffset.UTC);
  }
}
