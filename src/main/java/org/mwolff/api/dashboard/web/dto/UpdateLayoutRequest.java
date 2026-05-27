package org.mwolff.api.dashboard.web.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/** Layout-Update — ersetzt die komplette Widget-Liste eines Dashboards. */
public record UpdateLayoutRequest(@NotNull @Valid List<WidgetDto> widgets) {}
