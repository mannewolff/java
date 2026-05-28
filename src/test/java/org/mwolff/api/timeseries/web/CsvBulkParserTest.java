package org.mwolff.api.timeseries.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;

import org.junit.jupiter.api.Test;

class CsvBulkParserTest {

  @Test
  void parsesBareCsvWithoutHeader() {
    final var result =
        CsvBulkParser.parse("2026-05-27T12:00:00Z,78.5\n2026-05-28T12:00:00Z,79.0\n");

    assertThat(result.hasErrors()).isFalse();
    assertThat(result.rows()).hasSize(2);
    assertThat(result.rows().get(0).timestamp()).isEqualTo(Instant.parse("2026-05-27T12:00:00Z"));
    assertThat(result.rows().get(0).value()).isEqualByComparingTo("78.5");
  }

  @Test
  void detectsAndSkipsHeader() {
    final var result = CsvBulkParser.parse("timestamp,value\n2026-05-27T12:00:00Z,78.5\n");

    assertThat(result.hasErrors()).isFalse();
    assertThat(result.rows()).hasSize(1);
  }

  @Test
  void acceptsSpaceSeparatedDateTime() {
    final var result = CsvBulkParser.parse("2026-05-27 12:00:00,78.5\n");

    assertThat(result.hasErrors()).isFalse();
    assertThat(result.rows().get(0).timestamp()).isEqualTo(Instant.parse("2026-05-27T12:00:00Z"));
  }

  @Test
  void acceptsDateOnlyAtNoon() {
    final var result = CsvBulkParser.parse("2026-05-27,78.5\n");

    assertThat(result.hasErrors()).isFalse();
    assertThat(result.rows().get(0).timestamp()).isEqualTo(Instant.parse("2026-05-27T12:00:00Z"));
  }

  @Test
  void rejectsCommaInValue() {
    final var result = CsvBulkParser.parse("2026-05-27T12:00:00Z,\"78,5\"\n");

    assertThat(result.hasErrors()).isTrue();
    assertThat(result.errors().get(0).reason()).contains("decimal separator");
  }

  @Test
  void rejectsInvalidTimestamp() {
    // Erste Zeile ist Header-aehnlich -> Parser ueberspringt sie. Zweite Zeile hat
    // ungueltigen Timestamp.
    final var result = CsvBulkParser.parse("timestamp,value\nnicht-datum,78.5\n");

    assertThat(result.hasErrors()).isTrue();
    assertThat(result.errors().get(0).reason()).contains("invalid timestamp");
  }

  @Test
  void rejectsInvalidValue() {
    final var result = CsvBulkParser.parse("2026-05-27T12:00:00Z,nicht-zahl\n");

    assertThat(result.hasErrors()).isTrue();
    assertThat(result.errors().get(0).reason()).contains("invalid value");
  }

  @Test
  void rejectsExtraColumns() {
    final var result = CsvBulkParser.parse("2026-05-27T12:00:00Z,78.5,extra\n");

    assertThat(result.hasErrors()).isTrue();
    assertThat(result.errors().get(0).reason()).contains("found 3");
  }

  @Test
  void rejectsTooFewColumns() {
    final var result = CsvBulkParser.parse("2026-05-27T12:00:00Z\n");

    assertThat(result.hasErrors()).isTrue();
    assertThat(result.errors().get(0).reason()).contains("found 1");
  }

  @Test
  void rejectsTooManyRows() {
    final StringBuilder sb = new StringBuilder();
    for (int i = 0; i <= CsvBulkParser.MAX_ROWS; i++) {
      sb.append("2026-05-27T12:00:00Z,").append(i).append('\n');
    }

    final var result = CsvBulkParser.parse(sb.toString());

    assertThat(result.hasErrors()).isTrue();
    assertThat(result.errors().get(0).reason()).contains("max");
  }

  @Test
  void ignoresEmptyLines() {
    final var result = CsvBulkParser.parse("\n2026-05-27T12:00:00Z,1\n\n");

    assertThat(result.hasErrors()).isFalse();
    assertThat(result.rows()).hasSize(1);
    assertThat(result.rows().get(0).value()).isEqualByComparingTo(BigDecimal.ONE);
  }
}
