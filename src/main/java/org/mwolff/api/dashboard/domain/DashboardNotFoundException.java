package org.mwolff.api.dashboard.domain;

/**
 * Wird geworfen wenn ein Dashboard nicht existiert ODER nicht dem aufrufenden User gehört. Beide
 * Fälle werden bewusst gleich behandelt, damit der Server die Existenz fremder Dashboards nicht
 * leaked (verglichen mit der Alternative 403 Forbidden).
 */
public class DashboardNotFoundException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public DashboardNotFoundException(long id) {
    super("Dashboard " + id + " not found");
  }
}
