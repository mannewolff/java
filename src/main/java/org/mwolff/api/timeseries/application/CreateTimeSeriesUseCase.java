package org.mwolff.api.timeseries.application;

import org.mwolff.api.timeseries.domain.TimeSeries;
import org.mwolff.api.timeseries.domain.TimeSeriesDataType;
import org.mwolff.api.timeseries.domain.TimeSeriesPort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Legt eine neue Zeitreihe fuer den User an. */
@Component
public class CreateTimeSeriesUseCase {

  private final TimeSeriesPort timeSeries;

  public CreateTimeSeriesUseCase(TimeSeriesPort timeSeries) {
    this.timeSeries = timeSeries;
  }

  @Transactional
  public TimeSeries execute(
      String userSub, String name, String description, String unit, TimeSeriesDataType dataType) {
    return timeSeries.save(TimeSeries.newInstance(userSub, name, description, unit, dataType));
  }
}
