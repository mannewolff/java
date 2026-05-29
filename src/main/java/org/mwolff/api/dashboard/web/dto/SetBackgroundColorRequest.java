package org.mwolff.api.dashboard.web.dto;

import jakarta.validation.constraints.Size;

import org.mwolff.api.dashboard.domain.Dashboard;

/**
 * Setzt die Hintergrundfarbe eines Dashboards. {@code null} oder Leerwert entfernt den Override
 * (Frontend fällt dann auf den Theme-Default zurück).
 */
public record SetBackgroundColorRequest(
    @Size(max = Dashboard.MAX_BACKGROUND_COLOR_LENGTH) String backgroundColor) {}
