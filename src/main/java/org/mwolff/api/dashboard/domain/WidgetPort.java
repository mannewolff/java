package org.mwolff.api.dashboard.domain;

import java.util.List;

/**
 * Persistenz-Port für Widgets. Implementiert im Infrastructure-Layer via JPA.
 *
 * <p>Widgets gehören immer zu genau einem Dashboard und werden komplett ersetzt bei einer
 * Layout-Änderung (keine inkrementellen Updates).
 */
public interface WidgetPort {

  List<Widget> findAllByDashboard(long dashboardId);

  /**
   * Ersetzt alle Widgets des Dashboards: löscht die bisherigen, speichert die neuen. Aufruf muss
   * innerhalb einer Transaktion erfolgen.
   *
   * @return die neu gespeicherten Widgets mit ihren generierten IDs
   */
  List<Widget> replaceAllForDashboard(long dashboardId, List<Widget> widgets);

  void deleteByDashboard(long dashboardId);
}
