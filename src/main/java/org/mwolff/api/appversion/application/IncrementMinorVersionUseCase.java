package org.mwolff.api.appversion.application;

import org.mwolff.api.appversion.domain.AppVersion;
import org.mwolff.api.appversion.domain.AppVersionPort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Erhoeht die Minor-Version um eins und persistiert das Ergebnis. */
@Component
public class IncrementMinorVersionUseCase {

  private final AppVersionPort port;

  public IncrementMinorVersionUseCase(final AppVersionPort port) {
    this.port = port;
  }

  @Transactional
  public AppVersion execute() {
    final AppVersion next = port.getCurrent().withIncrementedMinor();
    port.setVersion(next);
    return next;
  }
}
