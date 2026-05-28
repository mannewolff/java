package org.mwolff.api.timeseries.web.dto;

import java.util.List;

/**
 * Antwort auf einen erfolgreichen Bulk-Import. {@code errors} sind die Zeilen-Fehler aus dem
 * CSV-Parser; auf Fehler wird mit 400 abgelehnt — dann ist {@code inserted} stets 0 und nichts
 * persistiert (all-or-nothing).
 */
public record BulkImportResponse(int inserted, List<BulkRowError> errors) {

  public record BulkRowError(long line, String reason) {}
}
