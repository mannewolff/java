package org.mwolff.api.dashboard.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import org.mwolff.api.dashboard.domain.Dashboard;

/** Anlegen eines neuen Dashboards. */
public record CreateDashboardRequest(
    @NotBlank @Size(max = Dashboard.MAX_NAME_LENGTH) String name) {}
