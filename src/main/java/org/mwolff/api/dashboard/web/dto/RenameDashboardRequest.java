package org.mwolff.api.dashboard.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import org.mwolff.api.dashboard.domain.Dashboard;

/** Umbenennen eines Dashboards — nur das Namensfeld wird angefasst. */
public record RenameDashboardRequest(
    @NotBlank @Size(max = Dashboard.MAX_NAME_LENGTH) String name) {}
