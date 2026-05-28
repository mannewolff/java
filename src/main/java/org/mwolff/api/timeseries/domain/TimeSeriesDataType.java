package org.mwolff.api.timeseries.domain;

/**
 * Datentyp einer Zeitreihe. Bestimmt die Wert-Validierung und die Anzeige in Widgets. Beide Typen
 * werden physisch als {@code DECIMAL(20,6)} gespeichert — {@link #INTEGER} ist nur ein semantischer
 * Sub-Type, der Nachkommastellen ablehnt.
 */
public enum TimeSeriesDataType {
  DECIMAL,
  INTEGER
}
