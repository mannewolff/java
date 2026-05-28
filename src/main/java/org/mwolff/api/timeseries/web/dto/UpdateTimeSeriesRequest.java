package org.mwolff.api.timeseries.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import org.mwolff.api.timeseries.domain.TimeSeries;
import org.mwolff.api.timeseries.domain.TimeSeriesDataType;

/** Aktualisiert die Metadaten einer Zeitreihe. */
public record UpdateTimeSeriesRequest(
    @NotBlank @Size(max = TimeSeries.MAX_NAME_LENGTH) String name,
    @Size(max = TimeSeries.MAX_DESCRIPTION_LENGTH) String description,
    @NotBlank @Size(max = TimeSeries.MAX_UNIT_LENGTH) String unit,
    @NotNull TimeSeriesDataType dataType) {}
