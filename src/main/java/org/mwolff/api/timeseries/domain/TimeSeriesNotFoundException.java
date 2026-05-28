package org.mwolff.api.timeseries.domain;

/**
 * Wird geworfen, wenn eine Zeitreihe nicht existiert ODER nicht dem aufrufenden User gehoert. Beide
 * Faelle werden bewusst gleich behandelt, damit der Server die Existenz fremder Zeitreihen nicht
 * leaked (vgl. Dashboard).
 */
public class TimeSeriesNotFoundException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public TimeSeriesNotFoundException(long id) {
    super("TimeSeries " + id + " not found");
  }
}
