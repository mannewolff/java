package org.mwolff.api.timeseries.application;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.mwolff.api.timeseries.domain.TimeSeries;
import org.mwolff.api.timeseries.domain.TimeSeriesDataType;
import org.mwolff.api.timeseries.domain.TimeSeriesEntry;
import org.mwolff.api.timeseries.domain.TimeSeriesEntryPort;
import org.mwolff.api.timeseries.domain.TimeSeriesNotFoundException;
import org.mwolff.api.timeseries.domain.TimeSeriesPort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Bulk-Insert von Eintraegen in eine Zeitreihe. Validiert Owner und DataType. All-or-nothing per
 * Transaktion — entweder gehen alle durch oder keiner.
 */
@Component
public class BulkAddEntriesUseCase {

  public static final int MAX_ROWS = 50_000;

  private final TimeSeriesPort timeSeries;
  private final TimeSeriesEntryPort entries;

  public BulkAddEntriesUseCase(TimeSeriesPort timeSeries, TimeSeriesEntryPort entries) {
    this.timeSeries = timeSeries;
    this.entries = entries;
  }

  /** Eingabe-Record fuer eine einzelne CSV-Zeile (timestamp + value). */
  public record BulkEntry(Instant timestamp, BigDecimal value) {
    public BulkEntry {
      Objects.requireNonNull(timestamp, "timestamp must not be null");
      Objects.requireNonNull(value, "value must not be null");
    }
  }

  @Transactional
  public int execute(String userSub, long timeSeriesId, List<BulkEntry> rows) {
    if (rows.size() > MAX_ROWS) {
      throw new IllegalArgumentException(
          "too many rows: " + rows.size() + " (max " + MAX_ROWS + ")");
    }
    final TimeSeries owned =
        timeSeries
            .findById(timeSeriesId)
            .filter(ts -> ts.userSub().equals(userSub))
            .orElseThrow(() -> new TimeSeriesNotFoundException(timeSeriesId));
    if (owned.dataType() == TimeSeriesDataType.INTEGER) {
      for (int i = 0; i < rows.size(); i++) {
        if (rows.get(i).value().stripTrailingZeros().scale() > 0) {
          throw new IllegalArgumentException(
              "row " + (i + 1) + ": value must not have decimals for INTEGER series");
        }
      }
    }
    final List<TimeSeriesEntry> toSave = new ArrayList<>(rows.size());
    for (BulkEntry row : rows) {
      toSave.add(TimeSeriesEntry.newInstance(owned.id(), row.timestamp(), row.value()));
    }
    final List<TimeSeriesEntry> saved = entries.saveAll(toSave);
    return saved.size();
  }
}
