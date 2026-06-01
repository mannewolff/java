package org.mwolff.api.appversion.domain;

/**
 * Reines Domain-Modell der Anwendungsversion (Major.Minor). Unveraenderlich — Inkremente liefern
 * eine neue Instanz.
 *
 * @param major Hauptversion
 * @param minor Nebenversion
 */
public record AppVersion(int major, int minor) {

  /** Factory-Methode. */
  public static AppVersion of(final int major, final int minor) {
    return new AppVersion(major, minor);
  }

  /** Kopie mit um eins erhoehter Minor-Version (z. B. 0.1 → 0.2). */
  public AppVersion withIncrementedMinor() {
    return new AppVersion(major, minor + 1);
  }

  /**
   * Kopie mit um eins erhoehter Major-Version und auf 0 zurueckgesetzter Minor (z. B. 0.99 → 1.0).
   */
  public AppVersion withIncrementedMajor() {
    return new AppVersion(major + 1, 0);
  }

  /** Formatiert als {@code "major.minor"}, z. B. {@code "0.1"}. */
  public String format() {
    return major + "." + minor;
  }
}
