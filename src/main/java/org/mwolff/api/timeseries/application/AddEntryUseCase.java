package org.mwolff.api.timeseries.application;

import java.math.BigDecimal;
import java.time.Instant;

import org.mwolff.api.timeseries.domain.TimeSeries;
import org.mwolff.api.timeseries.domain.TimeSeriesDataType;
import org.mwolff.api.timeseries.domain.TimeSeriesEntry;
import org.mwolff.api.timeseries.domain.TimeSeriesEntryPort;
import org.mwolff.api.timeseries.domain.TimeSeriesNotFoundException;
import org.mwolff.api.timeseries.domain.TimeSeriesPort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Fuegt einer Zeitreihe einen neuen Eintrag hinzu. Validiert je nach {@link TimeSeriesDataType},
 * dass der Wert das Format einhaelt — bei {@code INTEGER} werden Nachkommastellen abgelehnt.
 *
 * <p>Owner-Schutz erfolgt vor dem Insert: fremde Zeitreihen werden als {@link
 * TimeSeriesNotFoundException} (404) behandelt, damit keine Existenz geleaked wird.
 */
@Component
public class AddEntryUseCase {

  private final TimeSeriesPort timeSeries;
  private final TimeSeriesEntryPort entries;

  public AddEntryUseCase(TimeSeriesPort timeSeries, TimeSeriesEntryPort entries) {
    this.timeSeries = timeSeries;
    this.entries = entries;
  }

  @Transactional
  public TimeSeriesEntry execute(
      String userSub, long timeSeriesId, Instant timestamp, BigDecimal value) {
    final TimeSeries owned =
        timeSeries
            .findById(timeSeriesId)
            .filter(ts -> ts.userSub().equals(userSub))
            .orElseThrow(() -> new TimeSeriesNotFoundException(timeSeriesId));
    if (owned.dataType() == TimeSeriesDataType.INTEGER && value.stripTrailingZeros().scale() > 0) {
      throw new IllegalArgumentException("value must not have decimals for INTEGER time series");
    }
    return entries.save(TimeSeriesEntry.newInstance(owned.id(), timestamp, value));
  }
}
