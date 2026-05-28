package org.mwolff.api.timeseries.domain;

import java.time.Instant;
import java.util.Objects;

/**
 * Zeitreihe eines Users — Metadaten-Container fuer eine Folge von {@link TimeSeriesEntry}-Werten.
 *
 * @param id ID nach Speicherung (vor Erstinsert {@code null})
 * @param userSub Keycloak-{@code sub} des Eigentuemers
 * @param name max 200 Zeichen, nicht leer
 * @param description optional, max 500 Zeichen
 * @param unit max 50 Zeichen, nicht leer (z. B. "kg", "°C", "kWh")
 * @param dataType bestimmt die Wert-Validierung beim {@code AddEntry}
 * @param createdAt Zeitpunkt der Erstanlage
 * @param updatedAt Zeitpunkt der letzten Aenderung
 */
public record TimeSeries(
    Long id,
    String userSub,
    String name,
    String description,
    String unit,
    TimeSeriesDataType dataType,
    Instant createdAt,
    Instant updatedAt) {

  public static final int MAX_NAME_LENGTH = 200;
  public static final int MAX_DESCRIPTION_LENGTH = 500;
  public static final int MAX_UNIT_LENGTH = 50;

  public TimeSeries {
    Objects.requireNonNull(userSub, "userSub must not be null");
    if (userSub.isBlank()) {
      throw new IllegalArgumentException("userSub must not be blank");
    }
    Objects.requireNonNull(name, "name must not be null");
    if (name.isBlank()) {
      throw new IllegalArgumentException("name must not be blank");
    }
    if (name.length() > MAX_NAME_LENGTH) {
      throw new IllegalArgumentException("name must be at most " + MAX_NAME_LENGTH + " chars");
    }
    if (description != null && description.length() > MAX_DESCRIPTION_LENGTH) {
      throw new IllegalArgumentException(
          "description must be at most " + MAX_DESCRIPTION_LENGTH + " chars");
    }
    Objects.requireNonNull(unit, "unit must not be null");
    if (unit.isBlank()) {
      throw new IllegalArgumentException("unit must not be blank");
    }
    if (unit.length() > MAX_UNIT_LENGTH) {
      throw new IllegalArgumentException("unit must be at most " + MAX_UNIT_LENGTH + " chars");
    }
    Objects.requireNonNull(dataType, "dataType must not be null");
  }

  /** Erzeugt eine noch nicht persistierte Zeitreihe. */
  public static TimeSeries newInstance(
      String userSub, String name, String description, String unit, TimeSeriesDataType dataType) {
    return new TimeSeries(null, userSub, name, description, unit, dataType, null, null);
  }

  /** Erzeugt eine Kopie mit aktualisierten Metadaten — id, userSub und timestamps bleiben. */
  public TimeSeries withMetadata(
      String newName, String newDescription, String newUnit, TimeSeriesDataType newDataType) {
    return new TimeSeries(
        id, userSub, newName, newDescription, newUnit, newDataType, createdAt, updatedAt);
  }
}
