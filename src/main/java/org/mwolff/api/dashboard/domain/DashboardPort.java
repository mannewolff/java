package org.mwolff.api.dashboard.domain;

import java.util.List;
import java.util.Optional;

/**
 * Persistenz-Port für Dashboards. Implementiert im Infrastructure-Layer via JPA.
 *
 * <p>Methoden mit {@code userSub}-Parameter filtern serverseitig nach Eigentum — Aufrufer dürfen
 * sich nicht auf später eingefügte Owner-Checks verlassen, das wird in der Application-Schicht
 * abgedeckt. {@code findById}/{@code save}/{@code delete} sind owner-agnostisch und müssen vom
 * Use-Case mit einer expliziten Eigentumsprüfung umrahmt werden.
 */
public interface DashboardPort {

  List<Dashboard> findAllByUser(String userSub);

  Optional<Dashboard> findById(long id);

  Optional<Dashboard> findDefaultByUser(String userSub);

  Dashboard save(Dashboard dashboard);

  void deleteById(long id);

  /**
   * Setzt alle Default-Flaggen für einen User auf {@code false}. Wird vor dem Setzen eines neuen
   * Defaults aufgerufen, um die maximal-ein-Default-Regel zu erhalten.
   */
  void clearDefaultForUser(String userSub);
}
