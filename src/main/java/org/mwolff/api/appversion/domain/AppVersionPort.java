package org.mwolff.api.appversion.domain;

/** Port fuer das Lesen und Schreiben der aktuellen {@link AppVersion}. */
public interface AppVersionPort {

  /** Liest die aktuelle Version. */
  AppVersion getCurrent();

  /** Persistiert die uebergebene Version als neue aktuelle Version. */
  void setVersion(AppVersion version);
}
