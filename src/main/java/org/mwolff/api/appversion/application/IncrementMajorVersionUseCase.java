package org.mwolff.api.appversion.application;

import org.mwolff.api.appversion.domain.AppVersion;
import org.mwolff.api.appversion.domain.AppVersionPort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Erhoeht die Major-Version um eins, setzt die Minor-Version auf 0 und persistiert das Ergebnis.
 */
@Component
public class IncrementMajorVersionUseCase {

  private final AppVersionPort port;

  public IncrementMajorVersionUseCase(final AppVersionPort port) {
    this.port = port;
  }

  @Transactional
  public AppVersion execute() {
    final AppVersion next = port.getCurrent().withIncrementedMajor();
    port.setVersion(next);
    return next;
  }
}
