package org.mwolff.api.timeseries.domain;

/**
 * Wird geworfen, wenn der {@code dataType} einer Zeitreihe auf {@link TimeSeriesDataType#INTEGER}
 * gewechselt werden soll, obwohl Bestandseinträge einen Nachkommaanteil haben. Der Wechsel würde
 * die INTEGER-Invariante rückwirkend verletzen und wird daher mit HTTP 409 (Conflict) abgelehnt.
 */
public class TimeSeriesDataTypeConflictException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public TimeSeriesDataTypeConflictException(long timeSeriesId) {
    super(
        "Cannot switch time series "
            + timeSeriesId
            + " to INTEGER: existing entries have decimal values");
  }
}
