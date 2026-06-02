package org.mwolff.api.dashboard.domain;

import java.util.Map;

/**
 * Liefert, wie oft gespeicherte Bilder von Dashboard-Widgets referenziert werden (#202). Bewusster
 * Einstiegspunkt für die dokumentierte Cross-Modul-Kante {@code image → dashboard}: das Image-Modul
 * fragt hierüber die Nutzung ab, um Lösch-Schutz und „Benutzt in X Widgets" umzusetzen.
 */
public interface WidgetImageUsagePort {

  /** Anzahl der IMAGE-Widgets, die das Bild mit dieser id referenzieren. */
  long countByImageId(long imageId);

  /** Map (imageId → Anzahl) für alle referenzierten Bilder; nicht referenzierte fehlen. */
  Map<Long, Long> usageCounts();
}
