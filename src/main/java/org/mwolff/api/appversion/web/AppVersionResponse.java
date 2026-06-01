package org.mwolff.api.appversion.web;

import org.mwolff.api.appversion.domain.AppVersion;

/**
 * Web-DTO der Anwendungsversion.
 *
 * @param major Hauptversion
 * @param minor Nebenversion
 */
public record AppVersionResponse(int major, int minor) {

  /** Mappt das Domain-Modell auf das Web-DTO. */
  public static AppVersionResponse from(final AppVersion version) {
    return new AppVersionResponse(version.major(), version.minor());
  }

  /** Formatiert als {@code "major.minor"}, z. B. {@code "0.1"}. */
  public String formatted() {
    return major + "." + minor;
  }
}
