package org.mwolff.api.image.domain;

import java.util.Map;

/**
 * Liefert, wie oft Bilder andernorts referenziert werden (#202). Domain-Port des Image-Moduls; die
 * Implementierung holt die Information über die dokumentierte Kante {@code image → dashboard}. Hält
 * den Image-Manager frei von Wissen über die konkrete Referenzquelle.
 */
public interface ImageUsagePort {

  /** Anzahl der Verwendungen eines Bildes; {@code 0} = ungenutzt (löschbar). */
  long countUsages(long imageId);

  /** Map (imageId → Anzahl) für alle referenzierten Bilder; nicht referenzierte fehlen. */
  Map<Long, Long> usageCounts();
}
